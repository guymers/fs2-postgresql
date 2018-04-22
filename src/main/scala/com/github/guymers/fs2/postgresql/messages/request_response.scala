package com.github.guymers.fs2.postgresql.messages

import java.nio.charset.StandardCharsets

import cats.data.Writer
import cats.free.Free
import cats.instances.vector._
import cats.syntax.flatMap._
import com.github.guymers.fs2.postgresql.ErrorResponseException
import com.github.guymers.fs2.postgresql.PostgreSQLConnectionInfo
import scodec.Encoder
import shapeless.Coproduct
import shapeless.Poly1

/**
  * Models a message sent to the server and its expected response.
  */
sealed trait MessageRequestResponse[F <: FrontendSent, B] {
  def response: MessageResponse[B]
}

object MessageRequestResponse {

  def instance[F <: FrontendSent, B](implicit MR: MessageResponse[B]): MessageRequestResponse[F, B] = {
    new MessageRequestResponse[F, B] {
      override def response: MessageResponse[B] = MR
    }
  }

  implicit val startupRR: MessageRequestResponse[StartupMessage, MessageResponse.StartupResponses] = {
    instance[StartupMessage, MessageResponse.StartupResponses]
  }

  implicit val passwordRR: MessageRequestResponse[PasswordMessage, MessageResponse.PasswordResponses] = {
    instance[PasswordMessage, MessageResponse.PasswordResponses]
  }

  implicit val queryRR: MessageRequestResponse[Query, MessageResponse.QueryResponses] = {
    instance[Query, MessageResponse.QueryResponses]
  }

  def startup(msg: StartupMessage)(user: String, pass: String): MessageFlow.MessageFlow[Unit] = {
    import MessageFlow._
    import encoders._

    for {
      startupResponse <- sendReceive(msg)
      _ <- {
        object fold extends Poly1 {
          implicit val c1 = at[ErrorResponse] { m =>
            error[MessageResponse.PasswordResponses](ErrorResponseException(m))
          }
          implicit val c2 = at[AuthenticationMessage] {
            case AuthenticationMessage.Ok =>
              pure(Coproduct[MessageResponse.PasswordResponses](AuthenticationMessage.Ok))
            case AuthenticationMessage.MD5Password(salt) =>
              val m = PasswordMessage.md5(user, pass, Array(salt._1, salt._2, salt._3, salt._4))(StandardCharsets.UTF_8)
              send(m)
            case AuthenticationMessage.CleartextPassword =>
              val m = PasswordMessage(pass)
              send(m)
          }
          implicit val c3 = at[NegotiateProtocolVersion] { m =>
            // This message will be followed by an ErrorResponse or a message indicating the success or failure of authentication.
            error[MessageResponse.PasswordResponses](new RuntimeException("no supported 1")) // FIXME
          }
        }
        startupResponse.fold(fold)
      }
      _ <- {
        def go(): MessageFlow.MessageFlow[Unit] = {
          receive[MessageResponse.PasswordResponses].flatMap { r =>
            object fold extends Poly1 {
              implicit val c1 = at[AuthenticationMessage.Ok.type](_ => pure(()))
              implicit val c2 = at[ErrorResponse] { m =>
                error(ErrorResponseException(m)): MessageFlow.MessageFlow[Unit]
              }
              implicit val c3 = at[NoticeResponse](_ => go()) // ignore
              implicit val c4 = at[ParameterStatus](_ => go()) // ignore
            }
            r.fold(fold)
          }
        }
        go()
      }
    } yield ()
  }

  def initialization(): MessageFlow.MessageFlow[Writer[Vector[NoticeResponse], PostgreSQLConnectionInfo]] = {
    import MessageFlow._

    val response = receive[MessageResponse.AfterAuthResponses].flatMap { r =>
      r.select[ErrorResponse] match {
        case Some(e) => error[MessageResponse.AfterAuthResponses](ErrorResponseException(e))
        case None => pure(r)
      }
    }

    val responses = repeat(response) { r =>
      object fold extends Poly1 {
        implicit val c1 = at[BackendKeyData](_ => true)
        implicit val c2 = at[ParameterStatus](_ => true)
        implicit val c3 = at[ReadyForQuery](_ => false)
        implicit val c4 = at[ErrorResponse](_ => false)
        implicit val c5 = at[NoticeResponse](_ => true)
      }
      r.fold(fold)
    }

    responses.map { responses =>
      responses.foldLeft(Writer(Vector.empty[NoticeResponse], PostgreSQLConnectionInfo.empty)) { case (info, r) =>
        object fold extends Poly1 {
          implicit val c1 = at[BackendKeyData](m => info.map(_.setKeyData(m.processId, m.secretKey)))
          implicit val c2 = at[ParameterStatus](m => info.map(_.addParam(m.parameter, m.value)))
          implicit val c3 = at[ReadyForQuery](_ => info)
          implicit val c4 = at[ErrorResponse](_ => info)
          implicit val c5 = at[NoticeResponse](m => info.tell(Vector(m)))
        }
        r.fold(fold)
      }
    }
  }

  def query(msg: Query): MessageFlow.MessageFlow[Vector[MessageResponse.QueryResponses]] = {
    import MessageFlow._
    import encoders._

    val response = receive[MessageResponse.QueryResponses].flatMap { r =>
      // TODO maybe dont fail the stream?
      r.select[ErrorResponse] match {
        case Some(e) => error[MessageResponse.QueryResponses](ErrorResponseException(e))
        case None => pure(r)
      }
    }

    val responses = repeat(response) { r =>
      object fold extends Poly1 {
        implicit val c1 = at[CommandComplete](_ => true)
        implicit val c2 = at[EmptyQueryResponse.type](_ => true)
        implicit val c3 = at[ReadyForQuery](_ => false)
        implicit val c4 = at[ErrorResponse](_ => true) // "In the event of an error, ErrorResponse is issued followed by ReadyForQuery."
        implicit val c5 = at[NoticeResponse](_ => true)
        implicit val c6 = at[ParameterStatus](_ => true)
      }
      r.fold(fold)
    }

    send(msg) >> responses
  }

}

sealed trait MessageFlowF[R]
object MessageFlowF {
  final case class Pure[R](response: R) extends MessageFlowF[R]
  final case class Error[R](e: Throwable) extends MessageFlowF[R]
  final case class Receive[R](MR: MessageResponse[R]) extends MessageFlowF[R]
  final case class Send[M <: FrontendSent](msg: M, encoder: Encoder[M]) extends MessageFlowF[Unit]
  final case class SendReceive[M <: FrontendSent, R](msg: M, encoder: Encoder[M], MR: MessageRequestResponse[M, R]) extends MessageFlowF[R]
  final case class Repeat[R](action: MessageFlow.MessageFlow[R], until: R => Boolean) extends MessageFlowF[Vector[R]]
}

object MessageFlow {

  type MessageFlow[A] = Free[MessageFlowF, A]

  def pure[R](response: R): MessageFlow[R] = Free.liftF[MessageFlowF, R](MessageFlowF.Pure(response))
  def error[R](e: Throwable): MessageFlow[R] = Free.liftF[MessageFlowF, R](MessageFlowF.Error(e))
  def send[M <: FrontendSent](msg: M)(implicit E: Encoder[M]): MessageFlow[Unit] = {
    Free.liftF[MessageFlowF, Unit](MessageFlowF.Send(msg, E))
  }
  def receive[R](implicit MR: MessageResponse[R]): MessageFlow[R] = {
    Free.liftF[MessageFlowF, R](MessageFlowF.Receive(MR))
  }
  def sendReceive[M <: FrontendSent, R](msg: M)(implicit E: Encoder[M], MR: MessageRequestResponse[M, R]): MessageFlow[R] = {
    Free.liftF[MessageFlowF, R](MessageFlowF.SendReceive(msg, E, MR))
  }
  def repeat[R](action: MessageFlow[R])(until: R => Boolean): MessageFlow[Vector[R]] = {
    Free.liftF[MessageFlowF, Vector[R]](MessageFlowF.Repeat(action, until))
  }
}
