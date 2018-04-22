package com.github.guymers.fs2.postgresql.messages.decoders

import com.github.guymers.fs2.postgresql.messages.EmptyQueryResponse
import org.scalatest.FreeSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scodec.Attempt.Successful
import scodec.DecodeResult
import scodec.Decoder
import scodec.bits.BitVector

class EmptyQueryResponseDecoderSpec extends FreeSpec with GeneratorDrivenPropertyChecks {

  private val decoder = Decoder[EmptyQueryResponse.type]

  "decodes" in {
    val bits = BitVector.lowByte
    val result = decoder.decode(bits)
    assert(result == Successful(DecodeResult(EmptyQueryResponse, BitVector.lowByte)))
  }

}
