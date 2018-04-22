package com.github.guymers.fs2.postgresql.messages.encoders

import java.nio.charset.StandardCharsets

import com.github.guymers.fs2.postgresql.messages.MessageTestHelpers
import com.github.guymers.fs2.postgresql.messages.Query
import org.scalatest.FreeSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scodec.Attempt.Successful
import scodec.Encoder
import scodec.bits.BitVector

class QueryEncoderSpec extends FreeSpec with GeneratorDrivenPropertyChecks {

  private val encoder = Encoder[Query]

  "encodes" in {
    forAll { (query: String) =>
      val msg = Query(query)
      val bits = encoder.encode(msg)
      val expected = BitVector.fromByte('Q') ++
        BitVector.fromInt(query.getBytes(StandardCharsets.UTF_8).length + 1 + 4) ++
        MessageTestHelpers.utf8CString(query)
      assert(bits == Successful(expected))
    }
  }

}
