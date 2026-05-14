# Query: getSchoolSchema

This introspection query exposes exactly what inputs any given school accepts. The payload exposes `inputSchema` detailing variables like campuses or subjects matching valid enums, as well as `outputSchema` detailing precisely what internal blob items you will receive back.

```graphql
query {
  getSchoolSchema(school: "miami") {
  ... on SuccessSchoolSchema {
    school
    inputSchema
    outputSchema
  }
  ... on ErrorSchoolSchema {
      error
      message
    }
  }
}
```

**Example JSON Response `inputSchema` Segment**:
```json
{
  "campus": {
    "type": "array",
    "description": "Miami University campuses",
    "required": true,
    "options": [
      {
        "id": "O",
        "name": "Oxford"
      },
      {
        "id": "H",
        "name": "Hamilton"
      }
    ]
  }
}
```
