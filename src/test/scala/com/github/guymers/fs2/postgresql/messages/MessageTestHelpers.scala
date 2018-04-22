package com.github.guymers.fs2.postgresql.messages

import java.nio.charset.StandardCharsets

import org.scalacheck.Arbitrary
import scodec.bits.BitVector

object MessageTestHelpers {

  implicit val genValidPostgreSQLString: Arbitrary[String] = Arbitrary {
    Arbitrary.arbString.arbitrary.filter(s => s.nonEmpty && !s.contains(0: Char))
  }

  def utf8CString(str: String): BitVector = {
    BitVector(str.getBytes(StandardCharsets.UTF_8)) ++ BitVector.lowByte
  }
}
