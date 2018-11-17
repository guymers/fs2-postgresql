package com.github.guymers.fs2.postgresql

import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup

import scala.concurrent.duration.FiniteDuration

import cats.data.NonEmptyList
import cats.data.Writer
import cats.effect.ConcurrentEffect
import cats.effect.Sync
import cats.instances.string._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.foldable._
import cats.syntax.functor._
import cats.~>
import com.github.guymers.fs2.postgresql.messages.MessageResponse.NotifyResponses
import com.github.guymers.fs2.postgresql.messages._
import fs2.Chunk
import fs2.Chunk.ByteVectorChunk
import fs2.Stream
import fs2.io.tcp.Socket
import scodec.Attempt
import scodec.Decoder
import scodec.Encoder
import scodec.Err
import scodec.bits.BitVector
import scodec.bits.ByteVector
import shapeless.Poly1

final case class PostgreSQLClientParams(
  host: String,
  port: Int,
  user: String,
  pass: String,
  database: Option[String]
)

final case class PostgreSQLConnectionInfo(
  params: Map[String, String],
  processId: Int,
  secretKey: Int
) {
  def addParam(parameter: String, value: String): PostgreSQLConnectionInfo = {
    copy(params = params + (parameter -> value))
  }
  def setKeyData(processId: Int, secretKey: Int): PostgreSQLConnectionInfo = {
    copy(processId = processId, secretKey = secretKey)
  }
}

object PostgreSQLConnectionInfo {
  val empty = PostgreSQLConnectionInfo(Map.empty, 0, 0)
}

trait PostgreSQLConnection[F[_]] {

  /**
    * Information about the current connection.
    */
  def info: Writer[Vector[NoticeResponse], PostgreSQLConnectionInfo]

  /**
    * Listen to the given channels asynchronously.
    *
    * Each channel name is quoted.
    */
  def listen(channels: NonEmptyList[String]): Stream[F, NotificationResponse]
}

object PostgreSQLConnection {

  // buffer sizes used by postgres jdbc driver
  private val sendBufferSize = 8192
  private val receiveBufferSize = 8192

  /**
    * Create a connection to a PostgreSQL server.
    */
  def apply[F[_]](
    params: PostgreSQLClientParams,
    timeout: FiniteDuration
  )(implicit
    AG: AsynchronousChannelGroup,
    F: ConcurrentEffect[F]
  ): Stream[F, PostgreSQLConnection[F]] = {
    val address = new InetSocketAddress(params.host, params.port)

    for {
      socket <- Stream.resource {
        fs2.io.tcp.client(
          address,
          sendBufferSize = sendBufferSize,
          receiveBufferSize = receiveBufferSize
        )
      }
      info <- {
        val msg = StartupMessage(params.user, params.database, None, Map.empty)
        val prog = for {
          _ <- MessageRequestResponse.startup(msg)(params.user, params.pass)
          init <- MessageRequestResponse.initialization()
        } yield init

        prog.foldMap(run(socket, Some(timeout)))
      }
    } yield {
      val _info = info
      // TODO send Terminate when stream closes if server has not closed connection
      new PostgreSQLConnection[F] {

        val info: Writer[Vector[NoticeResponse], PostgreSQLConnectionInfo] = _info

        def listen(channels: NonEmptyList[String]): Stream[F, NotificationResponse] = {
          val commands = channels.map(channel => s"LISTEN ${PostgreSQLUtils.quotedIdentifier(channel)}")
          val msg = Query(commands.mkString_("BEGIN;", ";", ";COMMIT;"))

          // TODO send UNLISTEN on client close if connections are able to be reused?
          MessageRequestResponse.query(msg).foldMap(run(socket, Some(timeout))) >> {
            Stream.repeatEval(receive[F, NotifyResponses](socket, None)).flatMap { r =>
              object fold extends Poly1 {
                implicit val c1 = at[NotificationResponse](Stream.emit)
                implicit val c2 = at[ErrorResponse](m => Stream.raiseError(ErrorResponseException(m)))
                implicit val c3 = at[NoticeResponse](_ => Stream.empty)
                implicit val c4 = at[ParameterStatus](_ => Stream.empty)
              }
              r.fold(fold)
            }
          }
        }
      }
    }
  }

  private def run[F[_]: Sync](
    socket: Socket[F],
    timeout: Option[FiniteDuration]
  ): MessageFlowF ~> Stream[F, ?] = new (MessageFlowF ~> Stream[F, ?]) {

    def apply[A](fa: MessageFlowF[A]): Stream[F, A] = fa match {
      case MessageFlowF.Pure(result) =>
        Stream.emit(result)
      case MessageFlowF.Error(e) =>
        Stream.raiseError(e)
      case r: MessageFlowF.Receive[r] =>
        Stream.eval(receive[F, r](socket, timeout)(implicitly, r.MR))
      case r: MessageFlowF.Send[m] =>
        send[F, m](socket, timeout, r.msg)(implicitly, r.encoder)
      case r: MessageFlowF.SendReceive[m, r] =>
        sendAndReceiveMessage[F, m, r](socket, timeout, r.msg)(implicitly, r.encoder, r.MR)
      case r: MessageFlowF.Repeat[r] =>
        r.action.foldMap(run[F](socket, timeout)).repeat.takeWhile(r.until).fold(Vector.empty[r])(_ :+ _)
    }
  }

  private def sendAndReceiveMessage[F[_]: Sync, M <: FrontendSent : Encoder, R](
    socket: Socket[F],
    timeout: Option[FiniteDuration],
    msg: M
  )(implicit MR: MessageRequestResponse[M, R]): Stream[F, R] = for {
    _ <- send[F, M](socket, timeout, msg)
    result <- Stream.eval {
      receive(socket, timeout)(implicitly, MR.response)
    }
  } yield result

  private def send[F[_]: Sync, M <: FrontendSent](
    socket: Socket[F],
    timeout: Option[FiniteDuration],
    msg: M
  )(implicit E: Encoder[M]) = {
    for {
      bits <- Stream.eval[F, BitVector] {
        E.encode(msg).toEither.leftMap[Throwable](EncodingException).raiseOrPure[F]
      }
      _ <- Stream.chunk(ByteVectorChunk(bits.toByteVector)).covary[F]
        .to(socket.writes(timeout))
        .last
        .onFinalize(socket.endOfOutput)
    } yield ()
  }

  private def receive[F[_]: Sync, O](
    socket: Socket[F],
    timeout: Option[FiniteDuration]
  )(implicit MR: MessageResponse[O]): F[O] = for {
    headerChunk <- socket.readN(5, timeout) // message type byte + int32 containing the message length
    header <- decodeChunk[PostgreSQLMessageHeader](headerChunk)(messages.decoders.decoderMessageHeader).raiseOrPure[F]
    body <- socket.readN(header.length - 4, timeout) // length includes the size of the length field
    result <- decodeChunk[O](body)(MR.decoder(header)).raiseOrPure[F]
  } yield result

  private def decodeChunk[D: Decoder](chunk: Option[Chunk[Byte]]): Either[Throwable, D] = {
    attemptDecodeChunk[D](chunk).toEither.leftMap(DecodingException)
  }

  private def attemptDecodeChunk[D](chunk: Option[Chunk[Byte]])(implicit D: Decoder[D]) = {
    chunk match {
      case None => Attempt.failure(Err("chunk is empty"))
      case Some(c) => D.complete.decodeValue(chunk2ByteVector(c).toBitVector)
    }
  }

  // from fs2-http
  private def chunk2ByteVector(chunk: Chunk[Byte]): ByteVector = {
    chunk match  {
      case bv: ByteVectorChunk => bv.toByteVector
      case other =>
        val bytes = other.toBytes
        ByteVector(bytes.values, bytes.offset, bytes.size)
    }
  }

}
