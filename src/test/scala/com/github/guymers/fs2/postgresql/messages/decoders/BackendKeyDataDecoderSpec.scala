package com.github.guymers.fs2.postgresql.messages.decoders

import com.github.guymers.fs2.postgresql.messages.BackendKeyData
import org.scalatest.FreeSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scodec.Attempt.Successful
import scodec.DecodeResult
import scodec.Decoder
import scodec.bits.BitVector

class BackendKeyDataDecoderSpec extends FreeSpec with GeneratorDrivenPropertyChecks {

  private val decoder = Decoder[BackendKeyData]

  "decodes" in {
    forAll { (processId: Int, secretKey: Int) =>
      val bits = BitVector.fromInt(processId) ++ BitVector.fromInt(secretKey) ++ BitVector.lowByte
      val result = decoder.decode(bits)
      assert(result == Successful(DecodeResult(BackendKeyData(processId, secretKey), BitVector.lowByte)))
    }
  }

}
