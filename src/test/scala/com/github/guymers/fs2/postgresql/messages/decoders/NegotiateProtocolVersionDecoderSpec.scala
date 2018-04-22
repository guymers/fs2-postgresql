package com.github.guymers.fs2.postgresql.messages.decoders

import cats.instances.list._
import cats.syntax.foldable._
import org.scalatest.FreeSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scodec.Attempt.Successful
import scodec.DecodeResult
import scodec.Decoder
import scodec.bits.BitVector
import com.github.guymers.fs2.postgresql.instances.bitvector._
import com.github.guymers.fs2.postgresql.messages.MessageTestHelpers
import com.github.guymers.fs2.postgresql.messages.NegotiateProtocolVersion

class NegotiateProtocolVersionDecoderSpec extends FreeSpec with GeneratorDrivenPropertyChecks {
  import MessageTestHelpers.genValidPostgreSQLString

  private val decoder = Decoder[NegotiateProtocolVersion]

  "decodes" in {
    forAll { (minorVersion: Int, options: List[String]) =>
      val optionBits = options.map(MessageTestHelpers.utf8CString)
      val bits = BitVector.fromInt(minorVersion) ++ BitVector.fromInt(options.length) ++ optionBits.combineAll
      val result = decoder.decode(bits ++ BitVector.lowByte)
      assert(result == Successful(DecodeResult(NegotiateProtocolVersion(minorVersion, options), BitVector.lowByte)))
    }
  }

}
