package com.github.guymers.fs2.postgresql.messages

import scala.annotation.tailrec

import scodec.Attempt
import scodec.Codec
import scodec.DecodeResult
import scodec.Decoder
import scodec.Err
import scodec.SizeBound
import scodec.bits.BitVector
import scodec.codecs.filtered
import scodec.codecs.utf8
import scodec.codecs.vector

object Codecs {

  val NUL: BitVector = BitVector.lowByte

  // copy of cstring that uses utf8 instead of ascii
  val utf8CString: Codec[String] = filtered(utf8, new Codec[BitVector] {
    override def sizeBound: SizeBound = SizeBound.unknown
    override def encode(bits: BitVector): Attempt[BitVector] = Attempt.successful(bits ++ NUL)
    override def decode(bits: BitVector): Attempt[DecodeResult[BitVector]] = {
      bits.bytes.indexOfSlice(NUL.bytes) match {
        case -1 => Attempt.failure(Err("Does not contain a 'NUL' termination byte."))
        case i => Attempt.successful(DecodeResult(bits.take(i * 8L), bits.drop(i * 8L + 8L)))
      }
    }
  }).withToString("utf8 cstring")

  // repeat until a failure occurs
  def repeatUntilFailure[T](implicit C: Codec[T]): Codec[Vector[T]] = Codec(vector[T](C), repeatUntilFailureDecoder)

  private def repeatUntilFailureDecoder[T: Decoder]: Decoder[Vector[T]] = Decoder { bits =>
    @tailrec def go(bits: BitVector, errors: Vector[T]): Attempt[DecodeResult[Vector[T]]] = {
      Decoder.decode[T](bits) match {
        case Attempt.Failure(_) => Attempt.successful(DecodeResult(errors, bits))
        case Attempt.Successful(DecodeResult(value, remainder)) => go(remainder, errors :+ value)
      }
    }
    go(bits, Vector.empty)
  }
}
