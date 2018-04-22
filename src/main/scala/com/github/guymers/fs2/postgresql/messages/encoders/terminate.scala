package com.github.guymers.fs2.postgresql.messages.encoders

import com.github.guymers.fs2.postgresql.messages.Terminate
import scodec.Encoder
import scodec.codecs._

trait TerminateEncoder {
  private val MessageType: Byte = 'X'

  implicit val encoderTerminate: Encoder[Terminate.type] = {
    encode(MessageType, ignore(0).xmap(_ => Terminate, (_: Terminate.type) => ()))
  }
}
