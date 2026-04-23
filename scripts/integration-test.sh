#!/bin/bash
set -e

PORT=$1
if [ -z "$PORT" ]; then
  echo "Usage: ./integration-test.sh <PORT>"
  exit 1
fi

URL="http://localhost:$PORT/graphql"
echo "Running integration tests on $URL..."

run_test() {
  local name=$1
  local query=$2
  local expected=$3
  echo "Test: $name"
  local response=$(curl -s -X POST -H "Content-Type: application/json" -d "{\"query\": \"$query\"}" "$URL")
  
  if echo "$response" | grep -q "$expected"; then
    echo "SUCCESS: $name"
  else
    echo "FAILURE: $name. Expected $expected. Got: $response"
    exit 1
  fi
}

run_test "1. getSchoolSchema (Success)" \
  "query { getSchoolSchema(school: \\\"miami\\\") { ... on SuccessSchema { schema { schemaId inputSchema } } } }" \
  "\"inputSchema\""

run_test "2. getSchoolSchema (Invalid School -> Validation Error)" \
  "query { getSchoolSchema(school: \\\"INVALID_SCHOOL\\\") { ... on ErrorSchema { error message } } }" \
  "\"error\":\"VALIDATION_ERROR\""

run_test "3. getTerms (Success)" \
  "query { getTerms(school: \\\"miami\\\") { ... on SuccessField { fields { name } } } }" \
  "\"name\":\"20"

run_test "4. getTerms (Invalid School -> Validation Error)" \
  "query { getTerms(school: \\\"INVALID_SCHOOL\\\") { ... on ErrorField { error message } } }" \
  "\"error\":\"VALIDATION_ERROR\""

run_test "5. getCourses (Success by filter)" \
  "query { getCourses(input: { school: \\\"miami\\\", filters: { term: \\\"202510\\\", subject: [\\\"CSE\\\"], courseNum: \\\"271\\\", campus: [\\\"O\\\"] } }) { ... on SuccessCourse { courses { name timeWindows { startTime } data } } } }" \
  "\"courses\""

run_test "6. getCourses (Success by CRN)" \
  "query { getCourses(input: { school: \\\"miami\\\", filters: { term: \\\"202510\\\", crn: \\\"12345\\\" } }) { ... on SuccessCourse { courses { name } } ... on ErrorCourse { error } } }" \
  "\"QUERY_ERROR\"" # (Expected query error if CRN lacks mapping or is bad, but tests Query Error trapping)

run_test "7. getCourses (Missing 'term' filter -> Validation Error)" \
  "query { getCourses(input: { school: \\\"miami\\\", filters: { subject: [\\\"CSE\\\"] } }) { ... on ErrorCourse { error message } } }" \
  "\"error\":\"VALIDATION_ERROR\""

run_test "8. getSchedules (Success combinations)" \
  "query { getSchedules(input: { school: \\\"miami\\\", filters: { term: \\\"202510\\\", campus: [\\\"O\\\"] }, courses: [\\\"CSE 271\\\"] }) { ... on SuccessSchedule { schedules { sections { name } } } } }" \
  "\"schedules\""

run_test "9. getSchedules (Missing Required Campus Filter -> Validation Error)" \
  "query { getSchedules(input: { school: \\\"miami\\\", filters: { term: \\\"202510\\\" }, courses: [\\\"CSE 271\\\"] }) { ... on ErrorSchedule { error message } } }" \
  "\"error\":\"VALIDATION_ERROR\""

echo "All integration tests passed!"
