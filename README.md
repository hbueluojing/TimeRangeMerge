# TimeRangeMerge
## 1.divide the time range
### Registed data is:
 |Registed data      | StartDateTime        | EndDateTime     
 |          -------- | :-----------:        | :-----------: 
 |                 1 |"2019-02-01T00:00:00" | "2019-02-28T23:59:59"

### Added
 |Added data         | StartDateTime        | EndDateTime     
 |           --------| :-----------:        | :-----------: 
 |                 1 |"2019-02-10T00:00:00" | "2019-02-15T23:59:59"

### Result:
 |Result data        | StartDateTime        | EndDateTime          |
 |           --------| :-----------:        | :-----------:        |
 |                 1 |"2019-02-01T00:00:00" | "2019-02-09T23:59:59"|
 |                 2 |"2019-02-10T00:00:00" | "2019-02-15T23:59:59"|
 |                 3 |"2019-02-16T00:00:00" | "2019-02-28T23:59:59"|

## 2.shif the time range
### registed data is:
 |Registed data      | StartDateTime        | EndDateTime          |
 |          -------- | :-----------:        | :-----------:        |
 |                 1 |"2019-02-01T00:00:00" | "2019-02-28T23:59:59"|

### Added
 |Added data         | StartDateTime        | EndDateTime          |
 |           --------| :-----------:        | :-----------:        |
 |                 1 |"2019-02-10T00:00:00" | "2019-03-15T23:59:59"|

### Result:
 |Result data        | StartDateTime        | EndDateTime          |
 |           --------| :-----------:        | :-----------:        |
 |                 1 |"2019-02-01T00:00:00" | "2019-02-09T23:59:59"|
 |                 2 |"2019-02-10T00:00:00" | "2019-03-15T23:59:59"|

## 3.eclipse the time range
### registed data is:
 |Registed data      | StartDateTime        | EndDateTime          |
 |          -------- | :-----------:        | :-----------:        |
 |                 1 |"2019-02-01T00:00:00" | "2019-02-28T23:59:59"|

### Added 
 |Added data         | StartDateTime        | EndDateTime          |
 |           --------| :-----------:        | :-----------:        |
 |                 1 |"2019-01-10T00:00:00" | "2019-03-15T23:59:59"|

### Result:
 |Result data        | StartDateTime        | EndDateTime          |
 |           --------| :-----------:        | :-----------:        |
 |                 1 |"2019-01-10T00:00:00" | "2019-03-15T23:59:59"|

***
If the request list are overlapped,first make the request list unoverlapped ,then save them.

