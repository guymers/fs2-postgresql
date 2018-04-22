package com.github.guymers.fs2.postgresql.messages

import java.nio.charset.StandardCharsets

import org.scalatest.FreeSpec

class PasswordMessageSpec extends FreeSpec {

  "md5" - {

    "generates a password hash" in {
      val username = "postgres"
      val password = "password"
      val salt = "abcd".getBytes(StandardCharsets.UTF_8)
      val charset = StandardCharsets.UTF_8

      val msg = PasswordMessage.md5(username, password, salt)(charset)
      assert(msg.password == "md51133919977c5a2a5277421c17bef3716")
    }
  }
}
