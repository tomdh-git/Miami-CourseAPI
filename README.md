# Miami CourseAPI

[![CI](https://github.com/tomdh-git/Miami-CourseAPI/actions/workflows/gradle.yml/badge.svg)](https://github.com/tomdh-git/Miami-CourseAPI/actions/workflows/gradle.yml)

A SpringBoot GraphQL API for querying Miami University courses, generating conflict-free schedules, and discovering filler classes. Built with Kotlin, Netflix DGS, and the [IntervalCombinator](https://github.com/tomdh-git/interval-combinator) library.

## Architecture

```mermaid
graph TB
    subgraph Client
        GQL["GraphQL Client<br/>(POST /graphql)"]
    end

    subgraph "GraphQL Layer"
        CR["CourseResolver"]
        FR["FieldResolver"]
        SR["ScheduleResolver"]
    end

    subgraph "Exception Handling"
        EC["ExceptionConverter<br/>resolveQuery()"]
    end

    subgraph "Service Layer"
        CS["CourseService"]
        FS["FieldService"]
        SS["ScheduleService"]
    end

    subgraph "Validation"
        CV["CourseValidator"]
        SV["ScheduleValidator"]
    end

    subgraph "Schedule Engine"
        SC["ScheduleCombinator"]
        CM["CourseCombinatorMapper"]
        IC["IntervalCombinator<br/>(external lib)"]
        FAC["FillerAttributeCache"]
    end

    subgraph "School Abstraction"
        REG["SchoolRegistry"]
        CONN["SchoolConnector<br/>(interface)"]
    end

    subgraph "Miami Connector"
        MC["MiamiConnector"]
        CL["MiamiClient<br/>(WebClient + caching)"]
        FB["MiamiFormBuilder"]
        MP["MiamiParsers<br/>(Jsoup)"]
    end

    subgraph "External"
        MIAMI["Miami Course List<br/>apps.miamioh.edu/courselist"]
    end

    GQL -->|query| CR & FR & SR

    CR -->|wraps errors| EC
    FR -->|wraps errors| EC
    SR -->|wraps errors| EC

    CR --> CS
    FR --> FS
    SR --> SS

    CS -->|validate input| CV
    SS -->|validate input| SV

    CS --> REG
    FS --> REG
    SS --> REG

    SS --> SC

    REG -->|lookup by schoolId| CONN
    CONN -.->|implements| MC

    SC -->|map courses to intervals| CM
    CM --> IC
    SC -->|filler flow| FAC

    MC -->|fetch & parse| CL
    MC -->|parse HTML| MP
    CL -->|build POST body| FB
    CL -->|HTTP GET/POST| MIAMI

    style GQL fill:#4a9eff,color:#fff
    style MIAMI fill:#ff6b6b,color:#fff
    style IC fill:#ffa94d,color:#fff
    style EC fill:#845ef7,color:#fff
    style REG fill:#20c997,color:#fff
    style CONN fill:#20c997,color:#fff
```

### Query Flows

#### `getCourseByInfo` / `getCourseByCRN` — Course Lookup
```mermaid
sequenceDiagram
    participant C as Client
    participant R as CourseResolver
    participant S as CourseService
    participant V as CourseValidator
    participant REG as SchoolRegistry
    participant MC as MiamiConnector
    participant CL as MiamiClient
    participant M as Miami Server

    C->>R: GraphQL query
    R->>S: getCourseByInfo(input)
    S->>REG: getConnector(school)
    REG-->>S: MiamiConnector
    S->>V: validateCourseFields(input, validFields)
    Note over V: Throws IllegalArgumentException<br/>on invalid term/subject/campus
    S->>MC: getCourseByInfo(input)
    MC->>CL: getOrFetchToken()
    Note over CL: Returns cached CSRF token<br/>or fetches fresh one
    CL-->>MC: token
    MC->>CL: postResultResponse(formBody)
    CL->>M: POST /courselist (form data)
    M-->>CL: HTML response
    Note over CL: Handles 419/Page Expired<br/>with automatic token refresh
    CL-->>MC: HttpTextResponse
    MC->>MC: parseMiamiCourses(html)
    MC-->>S: List<Course>
    S-->>R: courses
    R-->>C: SuccessCourse / ErrorCourse
```

#### `getScheduleByCourses` — Schedule Generation
```mermaid
sequenceDiagram
    participant C as Client
    participant R as ScheduleResolver
    participant S as ScheduleService
    participant V as ScheduleValidator
    participant SC as ScheduleCombinator
    participant MC as MiamiConnector
    participant IC as IntervalCombinator

    C->>R: GraphQL query
    R->>S: getScheduleByCourses(input)
    S->>V: validateScheduleFields(input, validFields)
    S->>SC: getScheduleByCourses(input, connector)
    Note over SC: Parses "CSE 174" → ("CSE","174")
    loop Each requested course (async)
        SC->>MC: getCourseByInfo(query)
        MC-->>SC: List<Course> (all sections)
    end
    Note over SC: Verifies all courses found
    SC->>IC: IntervalCombinator.generate()
    Note over IC: Maps course meetings to<br/>time intervals, finds all<br/>non-conflicting combos
    IC-->>SC: List<CombinatorResult>
    SC-->>S: List<Schedule>
    S-->>R: schedules
    R-->>C: SuccessSchedule / ErrorSchedule
```

#### `getTerms` — Field Discovery
```mermaid
sequenceDiagram
    participant C as Client
    participant R as FieldResolver
    participant S as FieldService
    participant REG as SchoolRegistry
    participant MC as MiamiConnector
    participant CL as MiamiClient
    participant M as Miami Server

    C->>R: GraphQL query
    R->>S: getTerms(school)
    S->>REG: getConnector(school)
    REG-->>S: MiamiConnector
    S->>MC: getTerms()
    MC->>CL: getCourseList()
    Note over CL: Returns cached HTML<br/>or fetches fresh
    CL->>M: GET /courselist
    M-->>CL: HTML page
    CL-->>MC: html
    MC->>MC: parseMiamiTerms(html)
    MC-->>S: List<Field>
    S-->>R: terms
    R-->>C: SuccessField / ErrorField
```

### Key Design Decisions

| Component | Purpose |
|-----------|---------|
| **SchoolConnector** | Interface for multi-tenant support — add a new school by implementing this interface and registering as a Spring `@Component` |
| **SchoolRegistry** | Auto-discovers all `SchoolConnector` beans via Spring DI, routes requests by `schoolId` |
| **ExceptionConverter** | Catches all exceptions at the resolver boundary and maps them to typed GraphQL error responses (`VALIDATION_ERROR`, `QUERY_ERROR`, `API_ERROR`, `SERVER_BUSY`) |
| **MiamiClient** | Manages CSRF tokens, HTML caching, cookie state, and HTTP retries against Miami's server. Warms up on startup via `@PostConstruct` |
| **ScheduleCombinator** | Fetches all sections concurrently, maps them to time intervals, and delegates to `IntervalCombinator` for conflict-free schedule generation |
| **FillerAttributeCache** | Caches attribute-based course lookups for the filler flow to avoid redundant network calls |

## Testing & CI

This project implements a multi-layer testing strategy fully automated via GitHub Actions:

### 1. Static Analysis (Linting)
We use `ktlint` to enforce consistent Kotlin coding standards.
```bash
./gradlew ktlintCheck
```

### 2. Unit Tests
Uses JUnit 5, Mockito, and MockK for testing business logic, parsers, and validators in isolation.
```bash
./gradlew test
```

### 3. Integration Tests (Smoke Tests)
A "black-box" testing approach that:
1. Boots the full Spring Boot application on a random port.
2. Polls the `/health` endpoint until the system is ready.
3. Executes real GraphQL queries against `localhost` using `scripts/integration-test.sh`.
4. Verifies that real data is fetched from the Miami University servers.

To run manually:
```bash
./gradlew bootRun
# In another terminal:
./scripts/integration-test.sh <PORT_FROM_LOGS>
```

## Host
**WORK IN PROGRESS**
~~CourseAPI is currently being hosted at `https://courseapi-production-3751.up.railway.app`~~

## Requirements
- JDK 21
- Gradle 8.14+

## Local Build and Run

### 1. Clone the repository
```bash
git clone https://github.com/tomdh-git/Miami-CourseAPI.git
cd Miami-CourseAPI
```

### 2. Build the project
```bash
./gradlew clean build
```

### 3. Run the application
```bash
./gradlew bootRun
```
Then the application will be hosted at:
```
http://localhost:8080/graphql
```

## Example Queries

### [getCourseByInfo](https://github.com/tomdh-git/Miami-CourseAPI/blob/master/example_queries/getCourseByInfo.md)
### [getCourseByCRN](https://github.com/tomdh-git/Miami-CourseAPI/blob/master/example_queries/getCourseByCRN.md)
### [getScheduleByCourses](https://github.com/tomdh-git/Miami-CourseAPI/blob/master/example_queries/getScheduleByCourses.md)
### [getFillerByAttributes](https://github.com/tomdh-git/Miami-CourseAPI/blob/master/example_queries/getFillerByAttributes.md)
### [getTerms](https://github.com/tomdh-git/Miami-CourseAPI/blob/master/example_queries/getTerms.md)
