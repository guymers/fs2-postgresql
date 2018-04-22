package com.github.guymers.fs2.postgresql.instances

import cats.kernel.Monoid
import scodec.bits.BitVector

object bitvector {

  implicit val monoid: Monoid[BitVector] = new Monoid[BitVector] {
    override def empty: BitVector = BitVector.empty
    override def combine(x: BitVector, y: BitVector): BitVector = x ++ y
  }
}
