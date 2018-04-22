package com.github.guymers.fs2.postgresql.messages.decoders

import com.github.guymers.fs2.postgresql.messages.MessageTestHelpers
import com.github.guymers.fs2.postgresql.messages.NotificationResponse
import org.scalatest.FreeSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scodec.Attempt.Successful
import scodec.DecodeResult
import scodec.Decoder
import scodec.bits.BitVector

class NotificationResponseDecoderSpec extends FreeSpec with GeneratorDrivenPropertyChecks {
  import MessageTestHelpers.genValidPostgreSQLString

  private val decoder = Decoder[NotificationResponse]

  "decodes" in {
    forAll { (processId: Int, channel: String, payload: String) =>
      val bits = BitVector.fromInt(processId) ++
        MessageTestHelpers.utf8CString(channel) ++
        MessageTestHelpers.utf8CString(payload)
      val result = decoder.decode(bits ++ BitVector.lowByte)
      assert(result == Successful(DecodeResult(NotificationResponse(processId, channel, payload), BitVector.lowByte)))
    }
  }

}
