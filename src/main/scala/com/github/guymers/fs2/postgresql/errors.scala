package com.github.guymers.fs2.postgresql

import cats.syntax.show._
import scodec.Err
import com.github.guymers.fs2.postgresql.messages.ErrorResponse

@SuppressWarnings(Array("org.wartremover.warts.Null"))
sealed abstract class PostgreSQLException(msg: String, cause: Option[Throwable]) extends RuntimeException(msg, cause.orNull)

final case class DecodingException(err: Err) extends PostgreSQLException(s"Decoding error ${err.messageWithContext}", None)
final case class EncodingException(err: Err) extends PostgreSQLException(s"Encoding error ${err.messageWithContext}", None)
final case class ErrorResponseException(error: ErrorResponse) extends PostgreSQLException(
  show"PostgreSQL returned error: $error", None
)
