package com.github.guymers.fs2.postgresql.messages.decoders

import org.scalatest.FreeSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scodec.Attempt.Successful
import scodec.DecodeResult
import scodec.Decoder
import scodec.bits.BitVector
import com.github.guymers.fs2.postgresql.messages.CommandComplete
import com.github.guymers.fs2.postgresql.messages.MessageTestHelpers

class CommandCompleteDecoderSpec extends FreeSpec with GeneratorDrivenPropertyChecks {
  import MessageTestHelpers.genValidPostgreSQLString

  private val decoder = Decoder[CommandComplete]

  "decodes" in {
    forAll { (tag: String) =>
      val bits = MessageTestHelpers.utf8CString(tag) ++ BitVector.lowByte
      val result = decoder.decode(bits)
      assert(result == Successful(DecodeResult(CommandComplete(tag), BitVector.lowByte)))
    }
  }

}
