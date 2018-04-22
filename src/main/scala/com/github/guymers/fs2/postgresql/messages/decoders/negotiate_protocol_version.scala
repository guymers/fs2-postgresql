package com.github.guymers.fs2.postgresql.messages.decoders

import com.github.guymers.fs2.postgresql.messages.Codecs
import com.github.guymers.fs2.postgresql.messages.NegotiateProtocolVersion
import scodec.Decoder
import scodec.codecs._

trait NegotiateProtocolVersionDecoder {

  implicit val decoderNegotiateProtocolVersion: Decoder[NegotiateProtocolVersion] = {
    val minorVersion = "minor version" | int32
    val invalidProtocolOptions = "invalid protocol options" | listOfN(int32, Codecs.utf8CString)

    (minorVersion :: invalidProtocolOptions).as[NegotiateProtocolVersion]
  }
}
