package com.github.guymers.fs2.postgresql.messages.decoders

import scodec.Decoder
import scodec.codecs._
import com.github.guymers.fs2.postgresql.messages.BackendKeyData

trait BackendKeyDataDecoder {

  implicit val decoderBackendKeyData: Decoder[BackendKeyData] = {
    val processId = "process id" | int32
    val secretKey = "secret key" | int32

    (processId :: secretKey).as[BackendKeyData]
  }
}
