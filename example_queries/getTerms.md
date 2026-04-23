# Query: getTerms

This query retrieves a historical list of academic terms dynamically extracted from a school's registration backend.

```graphql
query {
  getTerms(school: "miami") {
    ... on SuccessField {
      fields {
        name
      }
    }
    ... on ErrorField {
      error
      message
    }
  }
}
```

**Example JSON ResponseSegment**:
```json
{
  "data": {
    "getTerms": {
      "fields": [
        { "name": "" },
        { "name": "202710" },
        { "name": "202630" }
      ]
    }
  }
}
```
