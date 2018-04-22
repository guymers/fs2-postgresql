package com.github.guymers.fs2.postgresql.messages.encoders

import java.nio.charset.StandardCharsets

import com.github.guymers.fs2.postgresql.messages.MessageTestHelpers
import com.github.guymers.fs2.postgresql.messages.PasswordMessage
import org.scalatest.FreeSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scodec.Attempt.Successful
import scodec.Encoder
import scodec.bits.BitVector

class PasswordMessageEncoderSpec extends FreeSpec with GeneratorDrivenPropertyChecks {

  private val encoder = Encoder[PasswordMessage]

  "encodes" in {
    forAll { (password: String) =>
      val msg = PasswordMessage(password)
      val bits = encoder.encode(msg)
      val expected = BitVector.fromByte('p') ++
        BitVector.fromInt(password.getBytes(StandardCharsets.UTF_8).length + 1 + 4) ++
        MessageTestHelpers.utf8CString(password)
      assert(bits == Successful(expected))
    }
  }

}
