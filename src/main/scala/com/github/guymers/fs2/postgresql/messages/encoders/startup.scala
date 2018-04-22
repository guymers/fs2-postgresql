package com.github.guymers.fs2.postgresql.messages.encoders

import com.github.guymers.fs2.postgresql.messages.Codecs
import com.github.guymers.fs2.postgresql.messages.StartupMessage
import scodec.Encoder
import scodec.bits.BitVector
import scodec.codecs._

trait StartupMessageEncoder {

  private val codec = {
    val nul = constant(BitVector.lowByte)
    val version = "protocol version" | (short16 ~ short16)
    val param = "param" | Codecs.utf8CString ~ Codecs.utf8CString
    val params = "params" | vector(param) <~ nul
    val data = version ~ params
    variableSizeBytes(int32, data, 4) // include the size field in the size
  }

  implicit val encoderStartupMessage: Encoder[StartupMessage] = Encoder { msg =>
    val params = msg.additionalParams ++ Map(
      "user" -> Some(msg.user),
      "database" -> msg.database,
      "replication" -> msg.replication
    ).collect { case (k, Some(v)) => k -> v }

    codec.encode((StartupMessage.Version, params.toVector))
  }
}
