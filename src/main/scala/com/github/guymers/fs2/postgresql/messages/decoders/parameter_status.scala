package com.github.guymers.fs2.postgresql.messages.decoders

import com.github.guymers.fs2.postgresql.messages.Codecs
import com.github.guymers.fs2.postgresql.messages.ParameterStatus
import scodec.Decoder
import scodec.codecs._

trait ParameterStatusDecoder {

  implicit val decoderParameterStatus: Decoder[ParameterStatus] = {
    val parameter = "parameter" | Codecs.utf8CString
    val value = "value" | Codecs.utf8CString

    (parameter :: value).as[ParameterStatus]
  }
}
