# Query: getSchedules

This query automatically generates combinations of sections resolving any overlaps. 
It accepts an array of desired courses and generates conflict-free schedules securely.

## Basic Schedule Combinations
```graphql
query {
  getSchedules(input: {
    school: "miami",
    filters: {
      term: "202410",
      campus: ["O"]
    },
    courses: ["ENG 109", "PHY 191"],
    preferredStart: "08:00am",
    preferredEnd: "08:00pm",
    optimizeFreeTime: true
  }) {
    ... on SuccessSchedule {
      schedules {
        freeTime
        sections {
          name
          timeWindows {
            day
            startTime
            endTime
          }
          data
        }
      }
    }
    ... on ErrorSchedule {
      error
      message
    }
  }
}
```

## Filler/Elective Gap Interpolation
By providing `fillerFilters`, the server will aggressively attempt to inject sections safely into the free gaps of the generated combination schedules.

```graphql
query {
  getSchedules(input: {
    school: "miami",
    filters: {
      term: "202410",
      campus: ["O"]
    },
    courses: ["CSE 271"],
    fillerFilters: {
      subject: ["MTH", "STA"],
      creditHours: 3
    }
  }) {
    ... on SuccessSchedule {
      schedules {
        freeTime
        sections {
          name
          data
        }
      }
    }
    ... on ErrorSchedule {
      error
      message
    }
  }
}
```
