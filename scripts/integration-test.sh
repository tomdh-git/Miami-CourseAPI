#!/bin/bash
set -e

PORT=$1
if [ -z "$PORT" ]; then
  echo "Usage: ./integration-test.sh <PORT>"
  exit 1
fi

URL="http://localhost:$PORT/graphql"
echo "Running integration tests on $URL..."

FAILURES=0
TOTAL=0

# ─────────────────────────────────────────────────────
# Helper: execute a GraphQL query and return the response
# ─────────────────────────────────────────────────────
gql() {
  local query=$1
  curl -s -X POST -H "Content-Type: application/json" \
    -d "{\"query\": \"$query\"}" "$URL"
}

# ─────────────────────────────────────────────────────
# Helper: run a named test with a jq assertion
#   $1 = test name
#   $2 = GraphQL query
#   $3 = jq expression that should produce a non-null, non-empty result
# ─────────────────────────────────────────────────────
run_test() {
  local name=$1
  local query=$2
  local jq_expr=$3
  TOTAL=$((TOTAL + 1))
  echo ""
  echo "Test: $name"

  local response
  response=$(gql "$query")

  local extracted
  extracted=$(echo "$response" | jq -r "$jq_expr" 2>/dev/null || echo "")

  if [ -n "$extracted" ] && [ "$extracted" != "null" ] && [ "$extracted" != "" ]; then
    echo "  ✅ PASS"
  else
    echo "  ❌ FAIL — jq expression '$jq_expr' returned nothing."
    echo "  Response: $response"
    FAILURES=$((FAILURES + 1))
  fi
}

# ─────────────────────────────────────────────────────
# Helper: run a test asserting a specific string equals the extracted value
#   $1 = test name
#   $2 = GraphQL query
#   $3 = jq expression
#   $4 = expected value
# ─────────────────────────────────────────────────────
run_test_eq() {
  local name=$1
  local query=$2
  local jq_expr=$3
  local expected=$4
  TOTAL=$((TOTAL + 1))
  echo ""
  echo "Test: $name"

  local response
  response=$(gql "$query")

  local extracted
  extracted=$(echo "$response" | jq -r "$jq_expr" 2>/dev/null || echo "")

  if [ "$extracted" = "$expected" ]; then
    echo "  ✅ PASS"
  else
    echo "  ❌ FAIL — Expected '$expected', got '$extracted'."
    echo "  Response: $response"
    FAILURES=$((FAILURES + 1))
  fi
}

echo ""
echo "═══════════════════════════════════════════════════"
echo " PHASE 1: Discovery — building dynamic test context"
echo "═══════════════════════════════════════════════════"

# ── 1. getSchoolSchema ──────────────────────────────
TOTAL=$((TOTAL + 1))
echo ""
echo "Test: 1. getSchoolSchema (discover inputSchema + outputSchema)"
SCHEMA_RESPONSE=$(gql "query { getSchoolSchema(school: \\\"miami\\\") { ... on SuccessSchoolSchema { school inputSchema outputSchema } ... on ErrorSchoolSchema { error message } } }")

SCHEMA_SCHOOL=$(echo "$SCHEMA_RESPONSE" | jq -r '.data.getSchoolSchema.school // empty' 2>/dev/null || echo "")
SCHEMA_INPUT=$(echo "$SCHEMA_RESPONSE" | jq -r '.data.getSchoolSchema.inputSchema // empty' 2>/dev/null || echo "")
SCHEMA_OUTPUT=$(echo "$SCHEMA_RESPONSE" | jq -r '.data.getSchoolSchema.outputSchema // empty' 2>/dev/null || echo "")

if [ -n "$SCHEMA_SCHOOL" ] && [ -n "$SCHEMA_INPUT" ] && [ -n "$SCHEMA_OUTPUT" ]; then
  echo "  ✅ PASS — school=$SCHEMA_SCHOOL, inputSchema and outputSchema present"
else
  echo "  ❌ FAIL"
  echo "  Response: $SCHEMA_RESPONSE"
  FAILURES=$((FAILURES + 1))
fi

# ── 2. getTerms (discover a valid term) ─────────────
TOTAL=$((TOTAL + 1))
echo ""
echo "Test: 2. getTerms (discover a valid term)"
TERMS_RESPONSE=$(gql "query { getTerms(school: \\\"miami\\\") { ... on SuccessField { fields { name } } ... on ErrorField { error message } } }")

# Pick the second term (index 1) — index 0 is often an empty sentinel in Miami's API
TERM=$(echo "$TERMS_RESPONSE" | jq -r '[.data.getTerms.fields[].name | select(length > 0)] | .[0] // empty' 2>/dev/null || echo "")

if [ -n "$TERM" ]; then
  echo "  ✅ PASS — Discovered term: $TERM"
else
  echo "  ❌ FAIL — Could not discover a valid term."
  echo "  Response: $TERMS_RESPONSE"
  echo "  Cannot continue — subsequent tests depend on a valid term."
  exit 1
fi

# ── 3. getCourses (discover a real course + CRN) ────
TOTAL=$((TOTAL + 1))
echo ""
echo "Test: 3. getCourses (discover real scheduled course data for term $TERM)"
COURSES_RESPONSE=$(gql "query { getCourses(input: { school: \\\"miami\\\", filters: { term: \\\"$TERM\\\", subject: [\\\"CSE\\\"], campus: [\\\"O\\\"] } }, limit: 20) { ... on SuccessCourse { courses { name data timeWindows { day } } } ... on ErrorCourse { error message } } }")

# Pick the first course that has timeWindows defined (so schedule combinator can actually schedule it)
COURSE_NAME_RAW=$(echo "$COURSES_RESPONSE" | jq -r '[.data.getCourses.courses[] | select(.timeWindows != null and (.timeWindows | length) > 0)] | .[0].name // empty' 2>/dev/null || echo "")
COURSE_CRN=$(echo "$COURSES_RESPONSE" | jq -r '[.data.getCourses.courses[] | select(.timeWindows != null and (.timeWindows | length) > 0)] | .[0].data.crn // empty' 2>/dev/null || echo "")
# Extract "CSE 148" format from "CSE 148 - Introduction to ..."
COURSE_SHORT=$(echo "$COURSE_NAME_RAW" | sed 's/ - .*//')

if [ -n "$COURSE_NAME_RAW" ] && [ -n "$COURSE_CRN" ]; then
  echo "  ✅ PASS — Discovered course: '$COURSE_SHORT' (CRN: $COURSE_CRN)"
else
  echo "  ❌ FAIL — Could not discover scheduled course data."
  echo "  Response: $COURSES_RESPONSE"
  echo "  Cannot continue — subsequent tests depend on discovered course data."
  exit 1
fi

echo ""
echo "═══════════════════════════════════════════════════"
echo " PHASE 2: Functional tests with discovered data"
echo "═══════════════════════════════════════════════════"
echo " Using: TERM=$TERM  COURSE=$COURSE_SHORT  CRN=$COURSE_CRN"

# ── 4. getCourses by filter (full fields) ───────────
run_test "4. getCourses (success — by subject filter with timeWindows)" \
  "query { getCourses(input: { school: \\\"miami\\\", filters: { term: \\\"$TERM\\\", subject: [\\\"CSE\\\"], campus: [\\\"O\\\"] } }) { ... on SuccessCourse { courses { name timeWindows { day startTime endTime } data } } ... on ErrorCourse { error message } } }" \
  '.data.getCourses.courses[0].name // empty'

# ── 5. getCourses by CRN ───────────────────────────
run_test "5. getCourses (success — by CRN)" \
  "query { getCourses(input: { school: \\\"miami\\\", filters: { term: \\\"$TERM\\\", crn: \\\"$COURSE_CRN\\\", campus: [\\\"O\\\"] } }) { ... on SuccessCourse { courses { name data } } ... on ErrorCourse { error message } } }" \
  '.data.getCourses.courses[0].name // empty'

# ── 6. getSchedules (success combinations) ──────────
run_test "6. getSchedules (success — schedule combinations)" \
  "query { getSchedules(input: { school: \\\"miami\\\", filters: { term: \\\"$TERM\\\", campus: [\\\"O\\\"] }, courses: [\\\"$COURSE_SHORT\\\"] }) { ... on SuccessSchedule { schedules { freeTime sections { name timeWindows { day startTime endTime } } } } ... on ErrorSchedule { error message } } }" \
  '.data.getSchedules.schedules[0].sections[0].name // empty'

echo ""
echo "═══════════════════════════════════════════════════"
echo " PHASE 3: Validation / Error contract tests"
echo "═══════════════════════════════════════════════════"

# ── 7. getSchoolSchema — invalid school ─────────────
run_test_eq "7. getSchoolSchema (invalid school -> VALIDATION_ERROR)" \
  "query { getSchoolSchema(school: \\\"INVALID_SCHOOL\\\") { ... on SuccessSchoolSchema { school } ... on ErrorSchoolSchema { error message } } }" \
  '.data.getSchoolSchema.error // empty' \
  "VALIDATION_ERROR"

# ── 8. getTerms — invalid school ────────────────────
run_test_eq "8. getTerms (invalid school -> VALIDATION_ERROR)" \
  "query { getTerms(school: \\\"INVALID_SCHOOL\\\") { ... on SuccessField { fields { name } } ... on ErrorField { error message } } }" \
  '.data.getTerms.error // empty' \
  "VALIDATION_ERROR"

# ── 9. getCourses — missing term ────────────────────
run_test_eq "9. getCourses (missing 'term' -> VALIDATION_ERROR)" \
  "query { getCourses(input: { school: \\\"miami\\\", filters: { subject: [\\\"CSE\\\"] } }) { ... on SuccessCourse { courses { name } } ... on ErrorCourse { error message } } }" \
  '.data.getCourses.error // empty' \
  "VALIDATION_ERROR"

# ── 10. getCourses — invalid school ─────────────────
run_test_eq "10. getCourses (invalid school -> VALIDATION_ERROR)" \
  "query { getCourses(input: { school: \\\"INVALID_SCHOOL\\\", filters: { term: \\\"$TERM\\\" } }) { ... on SuccessCourse { courses { name } } ... on ErrorCourse { error message } } }" \
  '.data.getCourses.error // empty' \
  "VALIDATION_ERROR"

# ── 11. getSchedules — missing campus ───────────────
run_test_eq "11. getSchedules (missing campus -> VALIDATION_ERROR)" \
  "query { getSchedules(input: { school: \\\"miami\\\", filters: { term: \\\"$TERM\\\" }, courses: [\\\"$COURSE_SHORT\\\"] }) { ... on SuccessSchedule { schedules { sections { name } } } ... on ErrorSchedule { error message } } }" \
  '.data.getSchedules.error // empty' \
  "VALIDATION_ERROR"

# ── 12. getSchedules — invalid school ───────────────
run_test_eq "12. getSchedules (invalid school -> VALIDATION_ERROR)" \
  "query { getSchedules(input: { school: \\\"INVALID_SCHOOL\\\", filters: { term: \\\"$TERM\\\", campus: [\\\"O\\\"] }, courses: [\\\"CSE 271\\\"] }) { ... on SuccessSchedule { schedules { sections { name } } } ... on ErrorSchedule { error message } } }" \
  '.data.getSchedules.error // empty' \
  "VALIDATION_ERROR"

# ─────────────────────────────────────────────────────
# Summary
# ─────────────────────────────────────────────────────
echo ""
echo "═══════════════════════════════════════════════════"
if [ $FAILURES -eq 0 ]; then
  echo " ✅ All $TOTAL integration tests passed!"
else
  echo " ❌ $FAILURES of $TOTAL tests FAILED."
  exit 1
fi
echo "═══════════════════════════════════════════════════"
