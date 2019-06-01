package com.github.spgrigorev.scodec.impl

import com.github.spgrigorev.scodec.algebras.Logger
import scalaz.zio.UIO

trait ConsoleLogger extends Logger {
  override val logger: Logger.Service = new Logger.Service {
    override def info(message: String): UIO[Unit] =
      UIO(System.out.println(s"INFO: $message"))

    override def error(error: String): UIO[Unit] =
      UIO(System.err.println(s"ERROR: $error"))
  }
}
