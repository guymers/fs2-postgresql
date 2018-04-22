package com.github.guymers.fs2.postgresql.messages

import scodec.Codec
import scodec.Encoder
import scodec.codecs._

package object encoders extends AllEncoders {

  def encode[T <: FrontendSent](messageType: Byte, codec: Codec[T]): Encoder[T] = {
    val c = byte ~ variableSizeBytes(int32, codec, 4) // include the size field in the size
    Encoder { t =>
      c.encode((messageType, t))
    }
  }
}
