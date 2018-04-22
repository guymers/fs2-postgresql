package com.github.guymers.fs2.postgresql.messages.decoders

import scala.annotation.switch

import cats.instances.int._
import cats.syntax.show._
import com.github.guymers.fs2.postgresql.messages.AuthenticationMessage
import scodec.Decoder
import scodec.Err
import scodec.codecs._

trait AuthenticationMessageDecoder {

  implicit val decoderAuthenticationMessage: Decoder[AuthenticationMessage] = {
    ("code" | int32).flatMap { code =>
      import com.github.guymers.fs2.postgresql.messages.AuthenticationMessage._

      (code: @switch) match {
        case 0 => ignore(0).map(_ => Ok)
        case 2 => fail(Err("KerberosV5 authentication method is not supported"))
        case 3 => ignore(0).map(_ => CleartextPassword)
        case 5 => (byte :: byte :: byte :: byte).map(bytes => MD5Password(bytes.tupled))
        case 6 => fail(Err("SCMCredential authentication method is not supported"))
        case 7 => fail(Err("GSS authentication method is not supported"))
        case 8 => fail(Err("GSSContinue authentication method is not supported"))
        case 9 => fail(Err("SSPI authentication method is not supported"))
        case 10 => fail(Err("SASL authentication method is not supported"))
        case 11 => fail(Err("SSASLContinueASL authentication method is not supported"))
        case 12 => fail(Err("SASLFinal authentication method is not supported"))
        case c => fail(Err(show"Authentication method with $c is not known"))
      }
    }
  }
}
