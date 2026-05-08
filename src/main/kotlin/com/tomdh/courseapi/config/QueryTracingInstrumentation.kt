package com.tomdh.courseapi.config

import graphql.ExecutionResult
import graphql.execution.instrumentation.InstrumentationContext
import graphql.execution.instrumentation.InstrumentationState
import graphql.execution.instrumentation.SimplePerformantInstrumentation
import graphql.execution.instrumentation.parameters.InstrumentationCreateStateParameters
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters
import graphql.schema.DataFetcher
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLObjectType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

/**
 * Logs execution time for every non-trivial datafetcher and the overall query.
 * Essential for diagnosing latency in coroutine-heavy, multi-connector pipelines.
 */
@Component
class QueryTracingInstrumentation : SimplePerformantInstrumentation() {

    private val logger = LoggerFactory.getLogger(QueryTracingInstrumentation::class.java)

    override fun createState(parameters: InstrumentationCreateStateParameters): InstrumentationState {
        return TraceState()
    }

    override fun beginExecution(
        parameters: InstrumentationExecutionParameters,
        state: InstrumentationState
    ): InstrumentationContext<ExecutionResult>? {
        require(state is TraceState)
        state.traceStartTime = System.currentTimeMillis()
        return super.beginExecution(parameters, state)
    }

    override fun instrumentDataFetcher(
        dataFetcher: DataFetcher<*>,
        parameters: InstrumentationFieldFetchParameters,
        state: InstrumentationState
    ): DataFetcher<*> {
        // Skip trivial property fetchers and introspection queries
        if (parameters.isTrivialDataFetcher ||
            parameters.executionStepInfo.path.toString().startsWith("/__schema")
        ) {
            return dataFetcher
        }

        val tag = resolveDataFetcherTag(parameters)

        return DataFetcher { environment ->
            val startTime = System.currentTimeMillis()
            val result = dataFetcher.get(environment)
            if (result is CompletableFuture<*>) {
                result.whenComplete { _, _ ->
                    val elapsed = System.currentTimeMillis() - startTime
                    logger.info("Async datafetcher '{}' took {}ms", tag, elapsed)
                }
            } else {
                val elapsed = System.currentTimeMillis() - startTime
                logger.info("Datafetcher '{}': {}ms", tag, elapsed)
            }
            result
        }
    }

    override fun instrumentExecutionResult(
        executionResult: ExecutionResult,
        parameters: InstrumentationExecutionParameters,
        state: InstrumentationState
    ): CompletableFuture<ExecutionResult> {
        require(state is TraceState)
        val totalTime = System.currentTimeMillis() - state.traceStartTime
        logger.info("Total query execution time: {}ms", totalTime)
        return super.instrumentExecutionResult(executionResult, parameters, state)
    }

    private fun resolveDataFetcherTag(parameters: InstrumentationFieldFetchParameters): String {
        val type = parameters.executionStepInfo.parent.type
        val parentType = if (type is GraphQLNonNull) {
            type.wrappedType as GraphQLObjectType
        } else {
            type as GraphQLObjectType
        }
        return "${parentType.name}.${parameters.executionStepInfo.path.segmentName}"
    }

    private data class TraceState(var traceStartTime: Long = 0) : InstrumentationState
}
