A very basic PostgreSQL driver built on top of [fs2](https://github.com/functional-streams-for-scala/fs2) in the spirit of
[fs2-http](https://github.com/Spinoco/fs2-http).

Currently only provides the ability to receive notifications.

Connection handling is very basic, receiving an error probably means a new connection should be created.

### Example ###
```scala
val params = PostgreSQLClientParams(
  host = "localhost",
  port = 5432,
  user = "postgres",
  pass = "postgres",
  database = None,
)

val conn = PostgreSQLConnection[F](params, timeout = 1.second)
conn.flatMap { conn =>

  val notifications: Stream[F, NotificationResponse] = conn.listen(NonEmptyList.of("test"))
}
```
