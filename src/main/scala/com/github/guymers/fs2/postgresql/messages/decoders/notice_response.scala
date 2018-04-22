package com.github.guymers.fs2.postgresql.messages.decoders

import cats.syntax.option._
import com.github.guymers.fs2.postgresql.messages.NoticeResponse
import scodec.Attempt
import scodec.Decoder
import scodec.Err

trait NoticeResponseDecoder {
  import PostgreSQLErrorNoticePart._

  // TODO shared code with ErrorResponseDecoder
  private val handledMessageTypes = Set(
    SQLSTATE,
    SEVERITY_LOCALIZED,
    MESSAGE,
    DETAIL,
    HINT,
    POSITION,
    WHERE,

    SCHEMA_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE_NAME,
    CONSTRAINT_NAME
  )

  implicit val decoderNoticeResponse: Decoder[NoticeResponse] = {
    partMapCodec.asDecoder.emap { parts =>
      for {
        sqlState <- Attempt.fromOption(parts.get(SQLSTATE), Err("Error does not contain SQLSTATE"))
        severity <- Attempt.fromOption(parts.get(SEVERITY_LOCALIZED), Err("Error does not contain SEVERITY"))
        message <- Attempt.fromOption(parts.get(MESSAGE), Err("Error does not contain MESSAGE"))
      } yield NoticeResponse(
        sqlState = sqlState,
        severity = severity,
        message = message,
        details = parts.get(DETAIL),
        hint = parts.get(HINT),
        position = parts.get(POSITION).flatMap(toInt),
        where = parts.get(WHERE),

        schemaName = parts.get(SCHEMA_NAME),
        tableName = parts.get(TABLE_NAME),
        columnName = parts.get(COLUMN_NAME),
        dataTypeName = parts.get(DATA_TYPE_NAME),
        constraintName = parts.get(CONSTRAINT_NAME),

        others = parts.filterKeys(key => !handledMessageTypes.contains(key))
      )
    }
  }

  private def toInt(str: String) = {
    try str.toInt.some catch { case _: NumberFormatException => None }
  }
}
