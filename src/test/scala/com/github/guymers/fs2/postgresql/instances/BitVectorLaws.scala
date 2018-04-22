package com.github.guymers.fs2.postgresql.instances

import cats.Eq
import cats.kernel.laws.discipline.MonoidTests
import com.github.guymers.fs2.postgresql.instances.bitvector._
import org.scalacheck.Arbitrary
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline
import scodec.bits.BitVector

object BitVectorLaws {

  implicit val arbitraryBitVector: Arbitrary[BitVector] = Arbitrary {
    Arbitrary.arbitrary[Array[Byte]].map(BitVector.apply)
  }

  implicit val eqBitVector: Eq[BitVector] = Eq.fromUniversalEquals
}

class BitVectorLaws extends FunSuite with Discipline {
  import BitVectorLaws._

  checkAll("monoid", MonoidTests[BitVector].monoid)
}
