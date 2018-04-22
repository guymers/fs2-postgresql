package com.github.guymers.fs2.postgresql.messages

import scodec.Attempt
import scodec.Decoder
import scodec.Err
import shapeless.:+:
import shapeless.CNil
import shapeless.Coproduct

sealed trait MessageResponse[B] {
  def decoder(header: PostgreSQLMessageHeader): Decoder[B]
}

object MessageResponse {
  import com.github.guymers.fs2.postgresql.messages.decoders.decoder

  def instance[B](f: PostgreSQLMessageHeader => Decoder[B]): MessageResponse[B] = new MessageResponse[B] {
    override def decoder(header: PostgreSQLMessageHeader): Decoder[B] = f(header)
  }

  // https://www.postgresql.org/docs/10/static/protocol-flow.html#PROTOCOL-ASYNC
  // ignore NotificationResponse as we will only get one when we call LISTEN
  type AsyncMessages = ErrorResponse :+: NoticeResponse :+: ParameterStatus :+: CNil

  // https://www.postgresql.org/docs/10/static/protocol-flow.html#id-1.10.5.7.3
  type StartupResponses = AuthenticationMessage :+: NegotiateProtocolVersion :+: ErrorResponse :+: CNil

  implicit val startup: MessageResponse[StartupResponses] = instance { header =>
    def failure(msg: String) = Attempt.failure(Err(s"Expected a startup response, received a $msg message"))

    decoder(header).emap {
      case m: AuthenticationMessage => Attempt.successful(Coproduct[StartupResponses](m))
      case m: NegotiateProtocolVersion => Attempt.successful(Coproduct[StartupResponses](m))
      case m: ErrorResponse => Attempt.successful(Coproduct[StartupResponses](m))

      case _: BackendKeyData => failure("BackendKeyData")
      case _: CommandComplete => failure("CommandComplete")
      case EmptyQueryResponse => failure("EmptyQueryResponse")
      case _: NoticeResponse => failure("NotificationResponse")
      case _: NotificationResponse => failure("NotificationResponse")
      case _: ParameterStatus => failure("ParameterStatus")
      case _: ReadyForQuery => failure("ReadyForQuery")
    }
  }

  type PasswordResponses = AuthenticationMessage.Ok.type :+: AsyncMessages

  implicit val password: MessageResponse[PasswordResponses] = instance { header =>
    def failure(msg: String) = Attempt.failure(Err(s"Expected a password response, received a $msg message"))

    decoder(header).emap {
      case AuthenticationMessage.Ok => Attempt.successful(Coproduct[PasswordResponses](AuthenticationMessage.Ok))

      case m: ErrorResponse => Attempt.successful(Coproduct[PasswordResponses](m))
      case m: NoticeResponse => Attempt.successful(Coproduct[PasswordResponses](m))
      case m: ParameterStatus => Attempt.successful(Coproduct[PasswordResponses](m))

      case AuthenticationMessage.CleartextPassword => failure("AuthenticationMessage.CleartextPassword")
      case _: AuthenticationMessage.MD5Password => failure("AuthenticationMessage.MD5Password")
      case _: BackendKeyData => failure("BackendKeyData")
      case _: CommandComplete => failure("CommandComplete")
      case EmptyQueryResponse => failure("EmptyQueryResponse")
      case _: NegotiateProtocolVersion => failure("NegotiateProtocolVersion")
      case _: NotificationResponse => failure("NotificationResponse")
      case _: ReadyForQuery => failure("ReadyForQuery")
    }
  }

  type AfterAuthResponses = BackendKeyData :+: ParameterStatus :+: ReadyForQuery :+: AsyncMessages

  implicit val auth: MessageResponse[AfterAuthResponses] = instance { header =>
    def failure(msg: String) = Attempt.failure(Err(s"Expected a after auth response, received a $msg message"))

    decoder(header).emap {
      case m: BackendKeyData => Attempt.successful(Coproduct[AfterAuthResponses](m))
      case m: ReadyForQuery => Attempt.successful(Coproduct[AfterAuthResponses](m))

      case m: ErrorResponse => Attempt.successful(Coproduct[AfterAuthResponses](m))
      case m: NoticeResponse => Attempt.successful(Coproduct[AfterAuthResponses](m))
      case m: ParameterStatus => Attempt.successful(Coproduct[AfterAuthResponses](m))

      case AuthenticationMessage.CleartextPassword => failure("AuthenticationMessage.CleartextPassword")
      case _: AuthenticationMessage.MD5Password => failure("AuthenticationMessage.MD5Password")
      case AuthenticationMessage.Ok => failure("AuthenticationMessage.Ok")
      case _: CommandComplete => failure("CommandComplete")
      case EmptyQueryResponse => failure("EmptyQueryResponse")
      case _: NegotiateProtocolVersion => failure("NegotiateProtocolVersion")
      case _: NotificationResponse => failure("NotificationResponse")
    }
  }

  // https://www.postgresql.org/docs/10/static/protocol-flow.html#id-1.10.5.7.4
  type QueryResponses = CommandComplete :+:
//    CopyInResponse :+:
//    CopyOutResponse :+:
//    RowDescription :+:
//    DataRow :+:
    EmptyQueryResponse.type :+:
    ReadyForQuery :+:
    AsyncMessages

  implicit val query: MessageResponse[QueryResponses] = instance { header =>
    def failure(msg: String) = Attempt.failure(Err(s"Expected a query response, received a $msg message"))

    decoder(header).emap {
      case m: CommandComplete => Attempt.successful(Coproduct[QueryResponses](m))
      case EmptyQueryResponse => Attempt.successful(Coproduct[QueryResponses](EmptyQueryResponse))
      case m: ReadyForQuery => Attempt.successful(Coproduct[QueryResponses](m))

      case m: ErrorResponse => Attempt.successful(Coproduct[QueryResponses](m))
      case m: NoticeResponse => Attempt.successful(Coproduct[QueryResponses](m))
      case m: ParameterStatus => Attempt.successful(Coproduct[QueryResponses](m))

      case AuthenticationMessage.CleartextPassword => failure("AuthenticationMessage.CleartextPassword")
      case _: AuthenticationMessage.MD5Password => failure("AuthenticationMessage.MD5Password")
      case AuthenticationMessage.Ok => failure("AuthenticationMessage.Ok")
      case _: BackendKeyData => failure("BackendKeyData")
      case _: NegotiateProtocolVersion => failure("NegotiateProtocolVersion")
      case _: NotificationResponse => failure("NotificationResponse")
    }
  }

  type NotifyResponses = NotificationResponse :+: AsyncMessages

  implicit val _notify: MessageResponse[NotifyResponses] = instance { header =>
    def failure(msg: String) = Attempt.failure(Err(s"Expected a notify response, received a $msg message"))

    decoder(header).emap {
      case m: NotificationResponse => Attempt.successful(Coproduct[NotifyResponses](m))

      case m: ErrorResponse => Attempt.successful(Coproduct[NotifyResponses](m))
      case m: NoticeResponse => Attempt.successful(Coproduct[NotifyResponses](m))
      case m: ParameterStatus => Attempt.successful(Coproduct[NotifyResponses](m))

      case AuthenticationMessage.CleartextPassword => failure("AuthenticationMessage.CleartextPassword")
      case _: AuthenticationMessage.MD5Password => failure("AuthenticationMessage.MD5Password")
      case AuthenticationMessage.Ok => failure("AuthenticationMessage.Ok")
      case _: BackendKeyData => failure("BackendKeyData")
      case _: CommandComplete => failure("CommandComplete")
      case EmptyQueryResponse => failure("EmptyQueryResponse")
      case _: NegotiateProtocolVersion => failure("NegotiateProtocolVersion")
      case _: ReadyForQuery => failure("ReadyForQuery")
    }
  }

}
