package com.github.guymers.fs2.postgresql.messages.decoders

import cats.syntax.show._
import com.github.guymers.fs2.postgresql.messages.ReadyForQuery
import com.github.guymers.fs2.postgresql.messages.ReadyForQuery.TransactionStatus
import org.scalatest.FreeSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scodec.Attempt.Failure
import scodec.Attempt.Successful
import scodec.DecodeResult
import scodec.Decoder
import scodec.Err
import scodec.bits.BitVector

class ReadyForQueryDecoderSpec extends FreeSpec with GeneratorDrivenPropertyChecks {

  private val decoder = Decoder[ReadyForQuery]

  private val statusMap = Map[Char, TransactionStatus](
    'I' -> TransactionStatus.Idle,
    'T' -> TransactionStatus.In,
    'E' -> TransactionStatus.Failed
  )

  "decodes" - {

    statusMap.foreach { case (char, status) =>
      show"$status status" in {
        val bits = BitVector.fromByte(char.toByte)
        val result = decoder.decode(bits ++ BitVector.lowByte)
        assert(result == Successful(DecodeResult(ReadyForQuery(status), BitVector.lowByte)))
      }
    }

    "an invalid status" in {
      forAll { (status: Byte) =>
        whenever(status != 0 && !statusMap.contains(status.toChar)) {
          val bits = BitVector.fromByte(status)
          val result = decoder.decode(bits ++ BitVector.lowByte)
          assert(result == Failure(Err(s"Could not convert '$status' into a transaction status")))
        }
      }
    }
  }

}
