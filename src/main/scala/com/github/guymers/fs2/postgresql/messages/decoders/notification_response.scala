package com.github.guymers.fs2.postgresql.messages.decoders

import com.github.guymers.fs2.postgresql.messages.Codecs
import com.github.guymers.fs2.postgresql.messages.NotificationResponse
import scodec.Decoder
import scodec.codecs._

trait NotificationResponseDecoder {

  implicit val decoderNotificationResponse: Decoder[NotificationResponse] = {
    val processId = "process id" | int32
    val channel = "channel" | Codecs.utf8CString
    val payload = "payload" | Codecs.utf8CString

    (processId :: channel :: payload).as[NotificationResponse]
  }
}
