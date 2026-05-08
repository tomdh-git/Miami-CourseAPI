import com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask

plugins {
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.netflix.dgs.codegen") version "7.0.3"
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
}

group = "com.tomdh"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencyManagement {
    imports {
        mavenBom("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:9.1.2")
    }
}

tasks.withType<GenerateJavaTask>().configureEach {
    schemaPaths = mutableListOf("${projectDir}/src/main/resources/schema")
    packageName = "com.tomdh.courseapi.generated"
    language = "kotlin"
    typeMapping = mutableMapOf(
        // Map GraphQL types to existing library classes instead of generating new ones
        "CourseSection" to "com.tomdh.schoolconnector.course.SchedulableSection",
        "TimeWindow" to "com.tomdh.schoolconnector.course.CanonicalTimeWindow",
        "Field" to "com.tomdh.schoolconnector.field.Field",
        // JSON scalar is handled by graphql-java-extended-scalars, not codegen
        "JSON" to "java.lang.Object"
    )
}

dependencies {
    // School Connector Library (transitively brings in session-aware-web-client)
    implementation("com.github.tomdh-git:school-connector:master-SNAPSHOT")

    // GraphQL - Netflix DGS
    implementation("com.netflix.graphql.dgs:graphql-dgs-spring-graphql-starter")
    implementation("com.graphql-java:graphql-java-extended-scalars:22.0")

    // Web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.1")
    implementation("org.springframework.data:spring-data-commons")

    // Caching
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")

    // Actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Testing
    testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")
    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("com.netflix.graphql.dgs:graphql-dgs-spring-graphql-starter-test")

    // Math & Combinatorics
    implementation("com.github.tomdh-git:interval-combinator:master-SNAPSHOT")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
