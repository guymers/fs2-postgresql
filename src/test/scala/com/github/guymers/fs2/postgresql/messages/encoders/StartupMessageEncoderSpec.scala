package com.github.guymers.fs2.postgresql.messages.encoders

import cats.instances.list._
import cats.syntax.foldable._
import com.github.guymers.fs2.postgresql.instances.bitvector._
import com.github.guymers.fs2.postgresql.messages.MessageTestHelpers
import com.github.guymers.fs2.postgresql.messages.StartupMessage
import org.scalatest.FreeSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scodec.Attempt.Successful
import scodec.Encoder
import scodec.bits.BitVector

class StartupMessageEncoderSpec extends FreeSpec with GeneratorDrivenPropertyChecks {

  private val encoder = Encoder[StartupMessage]

  "encodes" in {
    forAll { (user: String, database: Option[String], replication: Option[String], additionalParams: Map[String, String]) =>
      whenever(user.nonEmpty) {
        val msg = StartupMessage(user, database, replication, additionalParams)
        val bits = encoder.encode(msg)

        val params = additionalParams ++ Map(
          "user" -> Some(user),
          "database" -> database,
          "replication" -> replication
        ).collect { case (k, Some(v)) => k -> v }

        val body = BitVector.fromShort(StartupMessage.MajorVersion) ++
          BitVector.fromShort(StartupMessage.MinorVersion) ++
          params.toList.flatMap { case (a, b) => List(a, b) }.foldMap(MessageTestHelpers.utf8CString) ++
          BitVector.lowByte
        val expected = BitVector.fromInt(body.toByteVector.length.toInt + 4) ++ body
        assert(bits == Successful(expected))
      }
    }
  }

}
