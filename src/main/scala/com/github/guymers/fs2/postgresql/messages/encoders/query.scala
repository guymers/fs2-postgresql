package com.github.guymers.fs2.postgresql.messages.encoders

import com.github.guymers.fs2.postgresql.messages.Codecs
import com.github.guymers.fs2.postgresql.messages.Query
import scodec.Encoder
import scodec.codecs._

trait QueryEncoder {
  private val MessageType: Byte = 'Q'

  implicit val encoderQuery: Encoder[Query] = {
    val query = "query" | Codecs.utf8CString
    val codec = query.as[Query]
    encode(MessageType, codec)
  }
}
