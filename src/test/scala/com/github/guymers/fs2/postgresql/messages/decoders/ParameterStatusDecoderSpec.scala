package com.github.guymers.fs2.postgresql.messages.decoders

import com.github.guymers.fs2.postgresql.messages.MessageTestHelpers
import com.github.guymers.fs2.postgresql.messages.ParameterStatus
import org.scalatest.FreeSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scodec.Attempt.Successful
import scodec.DecodeResult
import scodec.Decoder
import scodec.bits.BitVector

class ParameterStatusDecoderSpec extends FreeSpec with GeneratorDrivenPropertyChecks {
  import MessageTestHelpers.genValidPostgreSQLString

  private val decoder = Decoder[ParameterStatus]

  "decodes" in {
    forAll { (parameter: String, value: String) =>
      val bits = MessageTestHelpers.utf8CString(parameter) ++
        MessageTestHelpers.utf8CString(value) ++
        BitVector.lowByte
      val result = decoder.decode(bits)
      assert(result == Successful(DecodeResult(ParameterStatus(parameter, value), BitVector.lowByte)))
    }
  }

}
