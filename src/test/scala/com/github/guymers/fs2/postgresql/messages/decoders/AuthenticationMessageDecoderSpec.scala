package com.github.guymers.fs2.postgresql.messages.decoders

import com.github.guymers.fs2.postgresql.messages.AuthenticationMessage
import org.scalatest.FreeSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scodec.Attempt.Failure
import scodec.Attempt.Successful
import scodec.DecodeResult
import scodec.Decoder
import scodec.Err
import scodec.bits.BitVector

class AuthenticationMessageDecoderSpec extends FreeSpec with GeneratorDrivenPropertyChecks {

  private val decoder = Decoder[AuthenticationMessage]

  "ok" - {
    "decodes" in {
      val bits = BitVector.fromInt(0) ++ BitVector.lowByte
      val result = decoder.decode(bits)
      assert(result == Successful(DecodeResult(AuthenticationMessage.Ok, BitVector.lowByte)))
    }
  }

  "clear text password" - {
    "decodes" in {
      val bits = BitVector.fromInt(3) ++ BitVector.lowByte
      val result = decoder.decode(bits)
      assert(result == Successful(DecodeResult(AuthenticationMessage.CleartextPassword, BitVector.lowByte)))
    }
  }

  "md5 password" - {
    "decodes" in {
      val salt: (Byte, Byte, Byte, Byte) = (23, 43, 76, 12)
      val bits = BitVector.fromInt(5) ++ BitVector(salt._1, salt._2, salt._3, salt._4) ++ BitVector.lowByte
      val result = decoder.decode(bits)
      result match {
        case Successful(DecodeResult(AuthenticationMessage.MD5Password(_salt), BitVector.lowByte)) => assert(salt == _salt)
        case r => assert(r == Successful(DecodeResult(AuthenticationMessage.MD5Password(salt), BitVector.lowByte)))
      }
    }
  }

  private val codes = Map(
    2 -> "KerberosV5",
    6 -> "SCMCredential",
    7 -> "GSS",
    8 -> "GSSContinue",
    9 -> "SSPI",
    10 -> "SASL",
    11 -> "SSASLContinueASL",
    12 -> "SASLFinal"
  )

  "unhandled" - {

    codes.foreach { case (code, authType) =>
      s"$authType" in {
        val bits = BitVector.fromInt(code)
        val result = decoder.decode(bits)
        assert(result == Failure(Err(s"$authType authentication method is not supported")))
      }
    }
  }

  "others" - {
    "decodes" in {
      val handledCodes = Set(0, 3, 5) ++ codes.keySet
      forAll { (code: Int) =>
        whenever (!handledCodes.contains(code)) {
          val bits = BitVector.fromInt(code)
          val result = decoder.decode(bits)
          assert(result == Failure(Err(s"Authentication method with ${code.toString} is not known")))
        }
      }
    }
  }

}
