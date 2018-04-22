package com.github.guymers.fs2.postgresql.messages.decoders

import com.github.guymers.fs2.postgresql.messages.Codecs
import com.github.guymers.fs2.postgresql.messages.CommandComplete
import scodec.Decoder
import scodec.codecs._

trait CommandCompleteDecoder {

  implicit val decoderCommandComplete: Decoder[CommandComplete] = {
    val tag = "tag" | Codecs.utf8CString

    tag.as[CommandComplete]
  }
}

