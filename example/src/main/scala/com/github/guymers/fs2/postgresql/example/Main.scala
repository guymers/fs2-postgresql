package com.github.guymers.fs2.postgresql.example

import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import cats.data.NonEmptyList
import cats.effect.IO
import com.github.guymers.fs2.postgresql.PostgreSQLClientParams
import com.github.guymers.fs2.postgresql.PostgreSQLConnection
import fs2.Scheduler
import fs2.Stream
import fs2.StreamApp
import fs2.StreamApp.ExitCode
import fs2.internal.ThreadFactories

object Main extends StreamApp[IO] {

  private implicit val ACG: AsynchronousChannelGroup = AsynchronousChannelGroup.withThreadPool {
    Executors.newCachedThreadPool(ThreadFactories.named("fs2-postgresql-ACG", daemon = false))
  }

  private implicit val EC: ExecutionContext = ExecutionContext.fromExecutor {
    Executors.newCachedThreadPool(ThreadFactories.named("fs2-postgresql-execution-context", daemon = true))
  }

  private implicit val S: Scheduler = Scheduler.fromScheduledExecutorService {
    Executors.newScheduledThreadPool(1, ThreadFactories.named("fs2-postgresql-scheduler", daemon = true))
  }

  def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, ExitCode] = {
    val params = PostgreSQLClientParams(
      host = "localhost",
      port = 5432,
      user = "postgres",
      pass = "postgres",
      database = None,
    )

    val conn = PostgreSQLConnection[IO](params, timeout = 5.seconds)
    conn.flatMap { conn =>

      val notifications = conn.listen(NonEmptyList.of("test")).observe {
        _.map { r =>
          println(r)
        }
      }

      val timeout = S.sleep[IO](30.seconds) >> Stream.emit(true)
      notifications.interruptWhen(timeout)
    }.drain
  }
}
