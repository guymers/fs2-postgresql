package com.github.guymers.fs2.postgresql

import cats.instances.char._
import cats.syntax.eq._

object PostgreSQLUtils {

  /**
    * Quote a PostgreSQL identifier by surrounding it in double quotes.
    *
    * Any double quotes in the string have an additional double quote added
    *
    * Does not error if the string contains the NUL byte.
    *
    * @see https://www.postgresql.org/docs/10/static/sql-syntax-lexical.html#SQL-SYNTAX-IDENTIFIERS
    */
  @SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
  def quotedIdentifier(str: String): String = {
    val sb = new StringBuilder(str.length + 2)
    sb.append('"')
    str.foreach { c =>
      if (c === '"') {
        sb.append('"')
      }
      sb.append(c)
    }
    sb.append('"')
    sb.toString()
  }
}
