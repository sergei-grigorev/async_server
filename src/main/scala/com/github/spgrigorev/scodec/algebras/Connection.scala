package com.github.spgrigorev.scodec.algebras

import com.github.spgrigorev.scodec.domain.ClientError
import scalaz.zio.{IO, UIO, ZIO}
import scodec.bits.BitVector

/**
  * Network connection.
  * @tparam CONNECTION type of connection
  */
trait Connection[CONNECTION] {
  def connection: Connection.Service[CONNECTION]
}

object Connection {

  trait Service[CONNECTION] {
    def read(buffer: BitVector,
             connection: CONNECTION): IO[ClientError, BitVector]
    def write(buffer: BitVector, connection: CONNECTION): IO[ClientError, Unit]
    def disconnect(connection: CONNECTION): UIO[Unit]
  }

  object algebra {
    def read[CONNECTION](buffer: BitVector, connection: CONNECTION)
      : ZIO[Connection[CONNECTION], ClientError, BitVector] =
      ZIO.accessM(_.connection.read(buffer, connection))

    def write[CONNECTION](connection: CONNECTION)(
        buffer: BitVector): ZIO[Connection[CONNECTION], ClientError, Unit] =
      ZIO.accessM(_.connection.write(buffer, connection))

    def disconnect[CONNECTION](
        connection: CONNECTION): ZIO[Connection[CONNECTION], Nothing, Unit] =
      ZIO.accessM(_.connection.disconnect(connection))
  }
}
