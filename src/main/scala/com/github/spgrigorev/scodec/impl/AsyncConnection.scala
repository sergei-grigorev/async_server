package com.github.spgrigorev.scodec.impl

import java.nio.ByteBuffer
import java.nio.channels.{
  AsynchronousSocketChannel,
  ClosedChannelException,
  CompletionHandler,
  ShutdownChannelGroupException
}

import com.github.spgrigorev.scodec.algebras.Connection
import com.github.spgrigorev.scodec.domain.{ClientError, ServerError}
import com.github.spgrigorev.scodec.impl.AsyncConnection.ClientSocket
import scalaz.zio.{IO, UIO, ZIO}

/**
  * This implementation doesn't support to write more than one
  * buffer at the same time.
  */
trait AsyncConnection extends Connection[ClientSocket, ByteBuffer] {

  override def connection: Connection.Service[ClientSocket, ByteBuffer] =
    new Connection.Service[ClientSocket, ByteBuffer] {
      override def read(buffer: ByteBuffer,
                        connection: ClientSocket): IO[ClientError, ByteBuffer] =
        ZIO
          .effectAsync { register =>
            connection.read(
              buffer,
              (),
              new CompletionHandler[Integer, Any] {
                override def completed(result: Integer, attachment: Any): Unit =
                  register {
                    if (result > 0)
                      UIO(buffer)
                    else
                      ZIO.fail(ClientError.ClosedConnection)
                  }

                override def failed(exc: Throwable, attachment: Any): Unit =
                  register(exc match {
                    case _: ShutdownChannelGroupException =>
                      ZIO.fail(ClientError.ClosedConnection)

                    case _: ClosedChannelException =>
                      ZIO.fail(ClientError.ClosedConnection)

                    // unexpected error, no workaround is available
                    case e: Throwable => ZIO.die(e)
                  })
              }
            )
          }

      override def write(buffer: ByteBuffer,
                         connection: ClientSocket): IO[ClientError, Unit] =
        ZIO
          .effectAsync { register =>
            connection.write(
              buffer,
              (),
              new CompletionHandler[Integer, Any] {
                override def completed(result: Integer, attachment: Any): Unit =
                  register {
                    if (result > 0)
                      UIO(buffer)
                    else
                      ZIO.fail(ClientError.ClosedConnection)
                  }

                override def failed(exc: Throwable, attachment: Any): Unit =
                  register(exc match {
                    case _: ShutdownChannelGroupException =>
                      ZIO.fail(ClientError.ClosedConnection)

                    case _: ClosedChannelException =>
                      ZIO.fail(ClientError.ClosedConnection)

                    // unexpected error, no workaround is available
                    case e: Throwable => ZIO.die(e)
                  })
              }
            )
          }

      override def disconnect(connection: ClientSocket): UIO[Unit] = {
        UIO(connection.close())
      }
    }
}

object AsyncConnection {
  type ClientSocket = AsynchronousSocketChannel
}
