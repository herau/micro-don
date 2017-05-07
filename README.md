# API Wrapper for Bankin' API

## Launch the project

```shell
sbt run
```

more information about [SBT installation](http://www.scala-sbt.org/index.html)

## Endpoints
 
 By default, the HTTP server listen on `localhost:9000

- `/round?email=xyz&password=foo`

> Returns the amount of the rounded transaction of the current accounts of a given user.

- `/rounds?since=abc&until=xyz`

> Return all users rounds transactions between two dates







