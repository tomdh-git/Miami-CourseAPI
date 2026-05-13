package com.tomdh.courseapi.instrumentation

import graphql.ExecutionResult
import graphql.execution.ResultPath
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.SimpleInstrumentationContext
import graphql.execution.instrumentation.SimplePerformantInstrumentation
import graphql.execution.instrumentation.parameters.InstrumentationCreateStateParameters
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters
import graphql.execution.instrumentation.parameters.InstrumentationValidationParameters
import graphql.language.Document
import graphql.schema.DataFetcher
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.validation.ValidationError
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

/**
 * Deep hierarchical query tracing instrumentation.
 *
 * Captures per-phase timing (parse, validation, execution) AND full field-resolution
 * tree with per-field timing, start offsets, fetcher class names, and critical path
 * highlighting. Essential for diagnosing latency in coroutine-heavy, multi-connector
 * pipelines — all without modifying any datafetcher or service file.
 */
@Component
class QueryTracingInstrumentation : SimplePerformantInstrumentation() {

    private val logger = LoggerFactory.getLogger(QueryTracingInstrumentation::class.java)

    // ── ANSI helpers ───────────────────────────────────────────────────────

    private object Ansi {
        const val RESET = "\u001B[0m"
        const val BOLD = "\u001B[1m"
        const val DIM = "\u001B[2m"
        const val CYAN = "\u001B[36m"
        const val YELLOW = "\u001B[33m"
        const val GREEN = "\u001B[32m"
        const val RED = "\u001B[31m"
        const val MAGENTA = "\u001B[35m"
        const val WHITE = "\u001B[97m"

        fun bold(s: String) = "$BOLD$s$RESET"
        fun dim(s: String) = "$DIM$s$RESET"
        fun cyan(s: String) = "$CYAN$s$RESET"
        fun yellow(s: String) = "$YELLOW$s$RESET"
        fun green(s: String) = "$GREEN$s$RESET"
        fun red(s: String) = "$RED$s$RESET"
        fun magenta(s: String) = "$MAGENTA$s$RESET"
        fun white(s: String) = "$WHITE$s$RESET"

        fun header(title: String) = bold(cyan("--- $title ---"))
        fun section(label: String, value: String) = "  ${dim(label)} $value"
    }

    // ── Phase lifecycle hooks ──────────────────────────────────────────────

    override fun createState(parameters: InstrumentationCreateStateParameters): InstrumentationState =
        TraceState()

    override fun beginParse(
        parameters: InstrumentationExecutionParameters,
        state: InstrumentationState
    ): InstrumentationContext<Document> {
        require(state is TraceState)
        val startNs = System.nanoTime()
        return SimpleInstrumentationContext.whenCompleted<Document> { _, _ ->
            state.parseDurationNs = System.nanoTime() - startNs
        }
    }

    override fun beginValidation(
        parameters: InstrumentationValidationParameters,
        state: InstrumentationState
    ): InstrumentationContext<List<ValidationError>> {
        require(state is TraceState)
        val startNs = System.nanoTime()
        return SimpleInstrumentationContext.whenCompleted<List<ValidationError>> { _, _ ->
            state.validationDurationNs = System.nanoTime() - startNs
        }
    }

    override fun beginExecution(
        parameters: InstrumentationExecutionParameters,
        state: InstrumentationState
    ): InstrumentationContext<ExecutionResult>? {
        require(state is TraceState)
        state.executionStartNs = System.nanoTime()
        return super.beginExecution(parameters, state)
    }

    // ── Field-level instrumentation ────────────────────────────────────────

    override fun instrumentDataFetcher(
        dataFetcher: DataFetcher<*>,
        parameters: InstrumentationFieldFetchParameters,
        state: InstrumentationState
    ): DataFetcher<*> {
        val resultPath = parameters.executionStepInfo.path

        if (resultPath.toString().startsWith(prefix = "/__schema")) return dataFetcher

        val isTrivial = parameters.isTrivialDataFetcher
        val tag = resolveDataFetcherTag(parameters)
        val fetcherClass = dataFetcher.javaClass.simpleName

        return DataFetcher { environment ->
            require(state is TraceState)
            val startNs = System.nanoTime()
            val startOffsetNs = startNs - state.executionStartNs

            val result: Any? = dataFetcher.get(environment)

            val recordTiming: (endNs: Long) -> Unit = { endNs ->
                val durationMs = (endNs - startNs) / 1_000_000.0
                val startOffsetMs = startOffsetNs / 1_000_000.0

                state.fieldTimings.add(
                    FieldTiming(
                        tag = tag,
                        resultPath = resultPath,
                        durationMs = durationMs,
                        startOffsetMs = startOffsetMs,
                        isTrivial = isTrivial,
                        fetcherClass = fetcherClass
                    )
                )

                if (!isTrivial) {
                    logger.info(
                        "  {} {} {}",
                        Ansi.yellow(tag),
                        Ansi.green(formatMs(durationMs)),
                        Ansi.dim("@+${formatMs(startOffsetMs)}")
                    )
                }
            }

            if (result is CompletableFuture<*>) {
                result.whenComplete { _, _ -> recordTiming(System.nanoTime()) }
            } else {
                recordTiming(System.nanoTime())
            }

            result
        }
    }

    // ── Final trace summary ────────────────────────────────────────────────

    override fun instrumentExecutionResult(
        executionResult: ExecutionResult,
        parameters: InstrumentationExecutionParameters,
        state: InstrumentationState
    ): CompletableFuture<ExecutionResult> {
        require(state is TraceState)

        val parseMs = state.parseDurationNs / 1_000_000.0
        val validationMs = state.validationDurationNs / 1_000_000.0
        val executionMs = (System.nanoTime() - state.executionStartNs) / 1_000_000.0
        val totalMs = parseMs + validationMs + executionMs

        val nonTrivialTimings = state.fieldTimings.filter { !it.isTrivial }
        val criticalPath = computeCriticalPath(state.fieldTimings)

        val summary = buildString {
            appendLine()
            appendLine(Ansi.header("Query Trace"))
            appendLine(Ansi.section("Parse:     ", Ansi.white(formatMs(parseMs))))
            appendLine(Ansi.section("Validate:  ", Ansi.white(formatMs(validationMs))))
            appendLine(Ansi.section("Execution: ", Ansi.white(formatMs(executionMs))))

            appendLine()
            appendLine(Ansi.header("Field Resolution Tree"))
            appendFieldTree(state.fieldTimings, this)

            appendLine()
            appendLine(Ansi.header("Non-Trivial Fetchers"))
            if (nonTrivialTimings.isNotEmpty()) {
                nonTrivialTimings.sortedByDescending { it.durationMs }.forEach { ft ->
                    val fetcherInfo = if (ft.fetcherClass != "PropertyDataFetcher")
                        Ansi.dim(" [${ft.fetcherClass}]") else ""
                    appendLine(
                        "  ${Ansi.yellow(ft.tag)}  ${Ansi.green(formatMs(ft.durationMs))}" +
                                "  ${Ansi.dim("@+${formatMs(ft.startOffsetMs)}")}$fetcherInfo"
                    )
                }
            } else {
                appendLine(Ansi.dim("  (none)"))
            }

            appendLine()
            appendLine(Ansi.header("Critical Path"))
            if (criticalPath.isNotEmpty()) {
                criticalPath.forEachIndexed { i, ft ->
                    val connector = if (i == criticalPath.lastIndex) "`--" else "|--"
                    appendLine(
                        "  ${Ansi.dim(connector)} ${Ansi.magenta(ft.tag)}  ${Ansi.red(formatMs(ft.durationMs))}"
                    )
                }
                val totalCriticalMs = criticalPath.sumOf { it.durationMs }
                appendLine("  ${Ansi.dim("total:")} ${Ansi.bold(Ansi.red(formatMs(totalCriticalMs)))}")
            } else {
                appendLine(Ansi.dim("  (none)"))
            }

            appendLine()
            append(Ansi.bold("Total: ${Ansi.green(formatMs(totalMs))}"))
        }

        logger.info(summary)

        return super.instrumentExecutionResult(executionResult, parameters, state)
    }

    // ── Tree builder ───────────────────────────────────────────────────────

    private fun appendFieldTree(timings: List<FieldTiming>, sb: StringBuilder) {
        val sorted = timings.sortedWith(compareBy({ it.startOffsetMs }, { it.resultPath.toString() }))
        for (ft in sorted) {
            val depth = (pathDepth(ft.resultPath) - 1).coerceAtLeast(0)
            val indent = "  " + "  ".repeat(depth)
            val connector = if (ft.isTrivial) Ansi.dim(".") else Ansi.cyan(">")
            val fetcherTag = if (!ft.isTrivial && ft.fetcherClass != "PropertyDataFetcher")
                Ansi.dim(" [${ft.fetcherClass}]") else ""
            val tagStr = if (ft.isTrivial) Ansi.dim(ft.tag) else Ansi.yellow(ft.tag)
            val timeStr = if (ft.isTrivial) Ansi.dim(formatMs(ft.durationMs)) else Ansi.green(formatMs(ft.durationMs))
            sb.appendLine("$indent$connector $tagStr  $timeStr  ${Ansi.dim("@+${formatMs(ft.startOffsetMs)}")}$fetcherTag")
        }
    }

    // ── Critical path computation ──────────────────────────────────────────

    private fun computeCriticalPath(timings: List<FieldTiming>): List<FieldTiming> {
        if (timings.isEmpty()) return emptyList()

        val pathToTimings = timings.associate { it.resultPath.toString() to it }

        data class PathResult(val path: List<FieldTiming>, val totalMs: Double)

        fun criticalFrom(path: String): PathResult {
            val current = pathToTimings[path] ?: return PathResult(emptyList(), 0.0)

            val children = timings.filter { other ->
                val otherStr = other.resultPath.toString()
                otherStr.startsWith("$path/") &&
                        pathDepth(other.resultPath) == pathDepth(ResultPath.parse(path)) + 1
            }

            if (children.isEmpty()) return PathResult(listOf(current), current.durationMs)

            val bestChild = children
                .map { criticalFrom(it.resultPath.toString()) }
                .maxByOrNull { it.totalMs }
                ?: return PathResult(listOf(current), current.durationMs)

            return PathResult(listOf(current) + bestChild.path, current.durationMs + bestChild.totalMs)
        }

        val roots = timings.filter { pathDepth(it.resultPath) == 1 }
        return roots
            .map { criticalFrom(it.resultPath.toString()) }
            .maxByOrNull { it.totalMs }
            ?.path ?: emptyList()
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun formatMs(durationMs: Double): String = String.format("%.1fms", durationMs)

    private fun pathDepth(path: ResultPath): Int =
        path.toString().count { it == '/' } + 1

    private fun resolveDataFetcherTag(parameters: InstrumentationFieldFetchParameters): String {
        val type: GraphQLOutputType? = parameters.executionStepInfo.parent.type
        val parentType = (if (type is GraphQLNonNull) type.wrappedType else type) as? GraphQLObjectType
            ?: error("Expected GraphQLObjectType but got $type")
        return "${parentType.name}.${parameters.executionStepInfo.path.segmentName}"
    }

    // ── Data structures ────────────────────────────────────────────────────

    private data class FieldTiming(
        val tag: String,
        val resultPath: ResultPath,
        val durationMs: Double,
        val startOffsetMs: Double,
        val isTrivial: Boolean,
        val fetcherClass: String
    )

    private data class TraceState(
        var parseDurationNs: Long = 0,
        var validationDurationNs: Long = 0,
        var executionStartNs: Long = 0,
        val fieldTimings: MutableList<FieldTiming> = mutableListOf()
    ) : InstrumentationState
}
