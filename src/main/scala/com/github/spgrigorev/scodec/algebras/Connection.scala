package com.github.spgrigorev.scodec.algebras

import com.github.spgrigorev.scodec.domain.ClientError
import scalaz.zio.{IO, UIO, ZIO}

/**
  * Network connection.
  * @tparam CONNECTION type of connection
  * @tparam BUFFER type of buffer used by IO operations
  */
trait Connection[CONNECTION, BUFFER] {
  def connection: Connection.Service[CONNECTION, BUFFER]
}

object Connection {

  trait Service[CONNECTION, BUFFER] {
    def read(buffer: BUFFER, connection: CONNECTION): IO[ClientError, BUFFER]
    def write(buffer: BUFFER, connection: CONNECTION): IO[ClientError, Unit]
    def disconnect(connection: CONNECTION): UIO[Unit]
  }

  object algebra {
    def read[CONNECTION, BUFFER](buffer: BUFFER, connection: CONNECTION)
      : ZIO[Connection[CONNECTION, BUFFER], ClientError, BUFFER] =
      ZIO.accessM(_.connection.read(buffer, connection))

    def write[CONNECTION, BUFFER](connection: CONNECTION)(buffer: BUFFER)
      : ZIO[Connection[CONNECTION, BUFFER], ClientError, Unit] =
      ZIO.accessM(_.connection.write(buffer, connection))

    def disconnect[CONNECTION](
        connection: CONNECTION): ZIO[Connection[CONNECTION, _], Nothing, Unit] =
      ZIO.accessM(_.connection.disconnect(connection))
  }
}
