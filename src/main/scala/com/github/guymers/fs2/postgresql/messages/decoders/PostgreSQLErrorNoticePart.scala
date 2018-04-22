package com.github.guymers.fs2.postgresql.messages.decoders

import com.github.guymers.fs2.postgresql.messages.Codecs
import scodec.Attempt
import scodec.Codec
import scodec.Decoder
import scodec.Err
import scodec.codecs._

// https://www.postgresql.org/docs/devel/static/protocol-error-fields.html
object PostgreSQLErrorNoticePart {

  val SEVERITY_LOCALIZED = 'S' // always present
  val SEVERITY = 'V' // always present in 9.6+
  val SQLSTATE = 'C' // always present
  val MESSAGE = 'M' // always present
  val DETAIL = 'D'
  val HINT = 'H'
  val POSITION = 'P'
  val INTERNAL_POSITION = 'p'
  val INTERNAL_QUERY = 'q'
  val WHERE = 'W'
  val SCHEMA_NAME = 's'
  val TABLE_NAME = 't'
  val COLUMN_NAME = 'c'
  val DATA_TYPE_NAME = 'd'
  val CONSTRAINT_NAME = 'n'
  val FILE = 'F'
  val LINE = 'L'
  val ROUTINE = 'R'

  implicit val partCodec: Codec[PostgreSQLErrorNoticePart] = {
    val tpe = ("type" | byte).exmap[Byte](
      byte => if (byte == 0) Attempt.failure(Err("Type cannot be NUL")) else Attempt.successful(byte),
      Attempt.successful
    ).xmapc(_.toChar)(_.toByte)
    val value = "value" | Codecs.utf8CString
    (tpe :: value).as[PostgreSQLErrorNoticePart]
  }

  // "Note that any given field type should appear at most once per message."
  implicit val partMapCodec: Decoder[Map[Char, String]] = {
    // vectorDelimited misses elements
    val partMap = Codecs.repeatUntilFailure[PostgreSQLErrorNoticePart].exmapc(
      parts => Attempt.successful(parts.map(part => part.tpe -> part.value).toMap)
    )(
      map => Attempt.successful(map.map((PostgreSQLErrorNoticePart.apply _).tupled).toVector)
    )
    partMap <~ constant(Codecs.NUL)
  }
}

final case class PostgreSQLErrorNoticePart(tpe: Char, value: String)
