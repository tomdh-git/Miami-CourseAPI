#!/bin/bash
set -e

PORT=$1
if [ -z "$PORT" ]; then
  echo "Usage: ./integration-test.sh <PORT>"
  exit 1
fi

URL="http://localhost:$PORT/graphql"

echo "Running integration tests on $URL..."

# 1. Test getTerms
echo "Test 1: getTerms (miami)"
RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" \
  -d '{"query": "{ getTerms(school: \"miami\") { ... on SuccessField { fields { name } } } }"}' \
  $URL)

if echo "$RESPONSE" | grep -q "SuccessField"; then
  echo "SUCCESS: getTerms returned SuccessField"
else
  echo "FAILURE: getTerms did not return SuccessField. Response: $RESPONSE"
  exit 1
fi

# 2. Test getCourseByInfo (Basic search)
echo "Test 2: getCourseByInfo (CSE 271, 202510)"
QUERY='{"query": "query { getCourseByInfo(input: { school: \"miami\", term: \"202510\", subject: \"CSE\", courseNum: \"271\", campus: [\"O\"] }) { ... on SuccessCourse { courses { subject courseNum crn } } } }"}'
RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" \
  -d "$QUERY" \
  $URL)

if echo "$RESPONSE" | grep -q "SuccessCourse"; then
  echo "SUCCESS: getCourseByInfo returned SuccessCourse"
else
  echo "FAILURE: getCourseByInfo did not return SuccessCourse. Response: $RESPONSE"
  exit 1
fi

echo "All integration tests passed!"
