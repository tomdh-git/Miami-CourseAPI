# Query: getCourses

This query unifies all course lookups. The filters are a dynamic JSON structure conforming to the target school's schema.

## Fetching by Info
Fetch courses matching specific criteria, such as all 'CSE' sections on the Oxford campus matching '465'.

```graphql
query {
  getCourses(input: {
    school: "miami",
    filters: {
      term: "202410",
      campus: ["O"],
      subject: ["CSE"],
      courseNum: "465"
    }
  }) {
    ... on SuccessCourse {
      courses {
        name
        timeWindows {
          day
          startTime
          endTime
        }
        data
      }
    }
    ... on ErrorCourse {
      error
      message
    }
  }
}
```

## Fetching by CRN
To fetch a specific section directly by its Course Reference Number (CRN), just provide the CRN key in the filter schema.

```graphql
query {
  getCourses(input: {
    school: "miami",
    filters: {
      term: "202410",
      crn: "12345"
    }
  }) {
    ... on SuccessCourse {
      courses {
        name
        timeWindows {
          day
          startTime
          endTime
        }
        data
      }
    }
    ... on ErrorCourse {
      error
      message
    }
  }
}
```
