package com.github.guymers.fs2.postgresql.messages.decoders

import java.nio.charset.StandardCharsets

import cats.data.NonEmptyVector
import cats.syntax.foldable._
import org.scalatest.FreeSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scodec.Attempt.Successful
import scodec.DecodeResult
import scodec.Decoder
import scodec.bits.BitVector
import com.github.guymers.fs2.postgresql.instances.bitvector._
import com.github.guymers.fs2.postgresql.messages.ErrorResponse

class ErrorResponseDecoderSpec extends FreeSpec with GeneratorDrivenPropertyChecks {

  private val decoder = Decoder[ErrorResponse]

  "decodes" in {
    val parts = NonEmptyVector.of(
      'S' -> "ERROR",
      'C' -> "42601",
      'M' -> """syntax error at or near "LISTEND"""",
      'P' -> "7",
      'F' -> "scan.l",
      'L' -> "1054",
      'R' -> "scanner_yyerror"
    )
    val bits = parts.foldMap((encode _).tupled) ++ BitVector.lowByte ++ BitVector.lowByte
    val result = decoder.decode(bits)
    val expected = ErrorResponse(
      sqlState = "42601",
      severity = "ERROR",
      message = """syntax error at or near "LISTEND"""",
      details = None,
      hint = None,
      position = Some(7),
      where = None,

      schemaName = None,
      tableName = None,
      columnName = None,
      dataTypeName = None,
      constraintName = None,

      others = Map(
        'F' -> "scan.l",
        'L' -> "1054",
        'R' -> "scanner_yyerror"
      )
    )
    assert(result == Successful(DecodeResult(expected, BitVector.lowByte)))
  }

  private def encode(tpe: Char, value: String) = {
    BitVector(tpe) ++ BitVector(value.getBytes(StandardCharsets.UTF_8)) ++ BitVector.lowByte
  }

}
