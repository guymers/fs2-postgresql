package com.github.guymers.fs2.postgresql.messages.encoders

import com.github.guymers.fs2.postgresql.messages.Codecs
import com.github.guymers.fs2.postgresql.messages.PasswordMessage
import scodec.Encoder
import scodec.codecs._

trait PasswordMessageEncoder {
  private val MessageType: Byte = 'p'

  implicit val encoderPasswordMessage: Encoder[PasswordMessage] = {
    val password = "password" | Codecs.utf8CString
    val codec = password.as[PasswordMessage]
    encode(MessageType, codec)
  }
}
