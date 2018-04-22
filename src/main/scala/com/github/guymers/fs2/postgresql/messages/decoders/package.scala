package com.github.guymers.fs2.postgresql.messages

import scala.annotation.switch

import scodec.Attempt
import scodec.Decoder
import scodec.Err
import scodec.codecs._

package object decoders extends AllDecoders {

  implicit val decoderMessageHeader: Decoder[PostgreSQLMessageHeader] = {
    val tpe = "type" | byte
    val length = "length" | int32
    (tpe :: length).as[PostgreSQLMessageHeader]
  }

  def decoder(header: PostgreSQLMessageHeader): Decoder[BackendSent] = {
    (header.tpe: @switch) match {
      case 'R' => Decoder[AuthenticationMessage]
      case 'K' => Decoder[BackendKeyData]
      case 'E' => Decoder[ErrorResponse]
      case 'N' => Decoder[NoticeResponse]
      case 'v' => Decoder[NegotiateProtocolVersion]
      case 'S' => Decoder[ParameterStatus]
      case 'Z' => Decoder[ReadyForQuery]
      case 'I' => Decoder[EmptyQueryResponse.type]
      case 'C' => Decoder[CommandComplete]
      case 'A' => Decoder[NotificationResponse]
      case tpe => Decoder(_ => Attempt.failure(Err(s"Message with type $tpe is not supported")))
    }
  }
}
