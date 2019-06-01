package com.github.spgrigorev.scodec.algebras

import com.github.spgrigorev.scodec.domain.ClientError
import scalaz.zio.{IO, UIO, ZIO}

/**
  * Network connection.
  * @tparam C type of connection
  * @tparam A type of buffer used by IO operations
  */
trait Connection[C, A] {
  def connection: Connection.Service[C, A]
}

object Connection {

  trait Service[C, A] {
    def read(buffer: A, connection: C): IO[ClientError, A]
    def write(buffer: A, connection: C): IO[ClientError, Unit]
    def disconnect(connection: C): UIO[Unit]
  }

  object algebra {
    def read[C, A](buffer: A,
                   connection: C): ZIO[Connection[C, A], ClientError, A] =
      ZIO.accessM(_.connection.read(buffer, connection))

    def write[C, A](buffer: A,
                    connection: C): ZIO[Connection[C, A], ClientError, Unit] =
      ZIO.accessM(_.connection.write(buffer, connection))

    def disconnect[C](connection: C): ZIO[Connection[C, _], Nothing, Unit] =
      ZIO.accessM(_.connection.disconnect(connection))
  }
}
