package com.github.guymers.fs2.postgresql.messages.decoders

import com.github.guymers.fs2.postgresql.messages.EmptyQueryResponse
import scodec.Decoder
import scodec.codecs._

trait EmptyQueryResponseDecoder {

  implicit val decoderEmptyQueryResponse: Decoder[EmptyQueryResponse.type] = {
    ignore(0).asDecoder.map(_ => EmptyQueryResponse)
  }
}

