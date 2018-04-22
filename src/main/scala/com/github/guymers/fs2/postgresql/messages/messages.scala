package com.github.guymers.fs2.postgresql.messages

import java.nio.charset.Charset
import java.security.MessageDigest

import cats.Show

final case class PostgreSQLMessageHeader(
  tpe: Byte,
  length: Int
)

sealed trait BackendSent
sealed trait FrontendSent

// https://www.postgresql.org/docs/devel/static/protocol-message-formats.html
sealed abstract class PostgreSQLMessage

object StartupMessage {
  val MajorVersion: Short = 3
  val MinorVersion: Short = 0
  val Version: (Short, Short) = (StartupMessage.MajorVersion, StartupMessage.MinorVersion)
}
final case class StartupMessage(
  user: String,
  database: Option[String],
  replication: Option[String],
  additionalParams: Map[String, String]
) extends FrontendSent

object PasswordMessage {

  def md5(username: String, password: String, salt: Array[Byte])(implicit C: Charset): PasswordMessage = {
    // SQL: concat('md5', md5(concat(md5(concat(password, username)), random-salt)))
    val usernamePassHash = {
      val md = MessageDigest.getInstance("MD5")
      md.update(password.getBytes(C))
      md.update(username.getBytes(C))
      md.digest()
    }
    val saltedHash = {
      val md = MessageDigest.getInstance("MD5")
      md.update(bytesToHex(usernamePassHash).getBytes(C))
      md.update(salt)
      md.digest()
    }
    PasswordMessage("md5" + bytesToHex(saltedHash))
  }

  private def bytesToHex(bytes: Array[Byte]) = bytes.map(0xFF & _).map("%02x".format(_)).mkString
}
final case class PasswordMessage(password: String) extends PostgreSQLMessage with FrontendSent

final case class Query(query: String) extends PostgreSQLMessage with FrontendSent

object Terminate extends PostgreSQLMessage with FrontendSent

// backend sent
sealed abstract class AuthenticationMessage extends PostgreSQLMessage with BackendSent
object AuthenticationMessage {
  case object Ok extends AuthenticationMessage
  case object CleartextPassword extends AuthenticationMessage
  final case class MD5Password(salt: (Byte, Byte, Byte, Byte)) extends AuthenticationMessage
}

object PostgreSQLErrorNotice {

  // same format as the toString in pgjdbc ServerErrorMessage
  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  implicit val show: Show[PostgreSQLErrorNotice] = Show.show { item =>
    val sb = new StringBuilder
    sb.append(s"${item.severity}: ${item.message}\n")
    item.details.foreach(s => sb.append(s"Detail: $s\n"))
    item.details.foreach(s => sb.append(s"Hint: $s\n"))
    item.details.foreach(s => sb.append(s"Position: $s\n"))
    item.details.foreach(s => sb.append(s"Where: $s\n"))
    sb.append(s"SQLState: ${item.sqlState}\n")
    sb.toString()
  }
}

sealed trait PostgreSQLErrorNotice {
  def sqlState: String
  def severity: String // TODO when only >=9.6 is supported turn this into an enum
  def message: String
  def details: Option[String]
  def hint: Option[String]
  def position: Option[Int]
  def where: Option[String]

  def schemaName: Option[String]
  def tableName: Option[String]
  def columnName: Option[String]
  def dataTypeName: Option[String]
  def constraintName: Option[String]

  def others: Map[Char, String]
}

object NoticeResponse {
  // currently NoticeResponse is the same as PostgreSQLErrorNotice
  implicit val show: Show[NoticeResponse] = {
    val show = Show[PostgreSQLErrorNotice]
    Show.show(show.show)
  }
}

final case class NoticeResponse(
  sqlState: String,
  severity: String,
  message: String,
  details: Option[String],
  hint: Option[String],
  position: Option[Int],
  where: Option[String],

  schemaName: Option[String],
  tableName: Option[String],
  columnName: Option[String],
  dataTypeName: Option[String],
  constraintName: Option[String],

  others: Map[Char, String]
) extends PostgreSQLMessage with BackendSent with PostgreSQLErrorNotice

object ErrorResponse {
  // currently NoticeResponse is the same as PostgreSQLErrorNotice
  implicit val show: Show[ErrorResponse] = {
    val show = Show[PostgreSQLErrorNotice]
    Show.show(show.show)
  }
}

final case class ErrorResponse(
  sqlState: String,
  severity: String,
  message: String,
  details: Option[String],
  hint: Option[String],
  position: Option[Int],
  where: Option[String],

  schemaName: Option[String],
  tableName: Option[String],
  columnName: Option[String],
  dataTypeName: Option[String],
  constraintName: Option[String],

  others: Map[Char, String]
) extends PostgreSQLMessage with BackendSent with PostgreSQLErrorNotice

final case class NegotiateProtocolVersion(
  minorVersion: Int,
  invalidProtocolOptions: List[String]
) extends PostgreSQLMessage with BackendSent

final case class BackendKeyData(
  processId: Int,
  secretKey: Int
) extends PostgreSQLMessage with BackendSent

final case class ParameterStatus(
  parameter: String,
  value: String
) extends PostgreSQLMessage with BackendSent

final case class ReadyForQuery(
  transactionStatus: ReadyForQuery.TransactionStatus
) extends PostgreSQLMessage with BackendSent

object ReadyForQuery {

  sealed abstract class TransactionStatus
  object TransactionStatus {
    object Idle extends TransactionStatus
    object In extends TransactionStatus
    object Failed extends TransactionStatus

    implicit val show: Show[TransactionStatus] = Show.show {
      case Idle => "idle"
      case In => "in"
      case Failed => "failed"
    }
  }
}

final case class CommandComplete(tag: String) extends PostgreSQLMessage with BackendSent

object EmptyQueryResponse extends PostgreSQLMessage with BackendSent

final case class NotificationResponse(
  processId: Int,
  channel: String,
  payload: String
) extends PostgreSQLMessage with BackendSent
