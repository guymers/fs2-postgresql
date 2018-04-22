package com.github.guymers.fs2.postgresql.messages.encoders

import com.github.guymers.fs2.postgresql.messages.Terminate
import org.scalatest.FreeSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scodec.Attempt.Successful
import scodec.Encoder
import scodec.bits.BitVector

class TerminateEncoderSpec extends FreeSpec with GeneratorDrivenPropertyChecks {

  private val encoder = Encoder[Terminate.type]

  "encodes" in {
    val msg = Terminate
    val bits = encoder.encode(msg)
    val expected = BitVector.fromByte('X') ++ BitVector.fromInt(4)
    assert(bits == Successful(expected))
  }

}
