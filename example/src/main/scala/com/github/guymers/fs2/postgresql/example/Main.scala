package com.github.guymers.fs2.postgresql.example

import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors

import scala.concurrent.duration._

import cats.data.NonEmptyList
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import com.github.guymers.fs2.postgresql.PostgreSQLClientParams
import com.github.guymers.fs2.postgresql.PostgreSQLConnection
import com.github.guymers.fs2.postgresql.messages.NotificationResponse
import fs2.Stream
import fs2.internal.FS2ThreadFactories

object Main extends IOApp {

  private implicit val ACG: AsynchronousChannelGroup = AsynchronousChannelGroup.withThreadPool {
    Executors.newCachedThreadPool(FS2ThreadFactories.named("fs2-postgresql-ACG", daemon = false))
  }

  override def run(args: List[String]): IO[ExitCode] = {
    stream().drain.compile.drain.map(_ => ExitCode.Success).guarantee(IO.delay(ACG.shutdown()))
  }

  private def stream(): Stream[IO, NotificationResponse] = {
    val params = PostgreSQLClientParams(
      host = "localhost",
      port = 5432,
      user = "postgres",
      pass = "postgres",
      database = None
    )

    val conn = PostgreSQLConnection[IO](params, timeout = 5.seconds)
    conn.flatMap { conn =>

      val notifications = conn.listen(NonEmptyList.of("test")).observe {
        _.map { r =>
          println(r)
        }
      }

      notifications.interruptAfter(30.seconds)
    }
  }
}
