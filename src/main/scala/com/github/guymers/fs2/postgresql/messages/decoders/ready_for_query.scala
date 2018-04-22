package com.github.guymers.fs2.postgresql.messages.decoders

import scala.annotation.switch

import cats.instances.byte._
import cats.syntax.option._
import cats.syntax.show._
import com.github.guymers.fs2.postgresql.messages.ReadyForQuery
import com.github.guymers.fs2.postgresql.messages.ReadyForQuery.TransactionStatus
import scodec.Attempt
import scodec.Decoder
import scodec.Err
import scodec.codecs._

trait ReadyForQueryDecoder {

  implicit val decoderReadyForQuery: Decoder[ReadyForQuery] = {
    val status = ("transaction status" | byte).asDecoder.emap { byte =>
      Attempt.fromOption(
        transactionStatusFromByte(byte),
        Err(s"Could not convert '${byte.show}' into a transaction status")
      )
    }

    status.map(status => ReadyForQuery(status))
  }

  private def transactionStatusFromByte(b: Byte): Option[TransactionStatus] = (b: @switch) match {
    case 'I' => TransactionStatus.Idle.some
    case 'T' => TransactionStatus.In.some
    case 'E' => TransactionStatus.Failed.some
    case _ => None
  }
}
