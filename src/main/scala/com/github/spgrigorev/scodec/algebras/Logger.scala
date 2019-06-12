package com.github.spgrigorev.scodec.algebras

import cats.Show
import cats.syntax.show._
import scalaz.zio.ZIO

trait Logger {
  def logger: Logger.Service
}

object Logger {
  trait Service {
    def info(message: String): ZIO[Logger, Nothing, Unit]
    def error(error: String): ZIO[Logger, Nothing, Unit]
    def warn(message: String): ZIO[Logger, Nothing, Unit]
  }

  object algebra {
    def info[S: Show](message: S): ZIO[Logger, Nothing, Unit] =
      ZIO.accessM(_.logger.info(message.show))

    def info[S: Show](message: String, content: S): ZIO[Logger, Nothing, Unit] =
      ZIO.accessM(_.logger.info(message + content.show))

    def error[S: Show](error: S): ZIO[Logger, Nothing, Unit] =
      ZIO.accessM(_.logger.error(error.show))

    def error[S: Show](message: String,
                       content: S): ZIO[Logger, Nothing, Unit] =
      ZIO.accessM(_.logger.error(message + content.show))

    def warn[S: Show](message: String, content: S): ZIO[Logger, Nothing, Unit] =
      ZIO.accessM(_.logger.warn(message + content.show))
  }
}
