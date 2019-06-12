package com.github.spgrigorev.scodec.impl

import java.nio.ByteBuffer
import java.nio.channels._

import com.github.spgrigorev.scodec.algebras.Connection
import com.github.spgrigorev.scodec.domain.ClientError
import com.github.spgrigorev.scodec.impl.AsyncConnection.ClientSocket
import eu.timepit.refined.auto._
import eu.timepit.refined.types.numeric.NonNegInt
import scalaz.zio.{IO, UIO, ZIO}
import scodec.bits.BitVector

import scala.concurrent.duration._

/**
  * This implementation doesn't support to write more than one
  * buffer at the same time.
  */
trait AsyncConnection extends Connection[ClientSocket] {

  override def connection: Connection.Service[ClientSocket] =
    new Connection.Service[ClientSocket] {

      val readTimeout: Duration = 30.seconds
      val writeTimeout: Duration = 10.seconds
      val blockSize: NonNegInt = 512

      override def read(buffer: BitVector,
                        connection: ClientSocket): IO[ClientError, BitVector] =
        ZIO
          .effectAsync { register =>
            // allocate buffer that contains [[buffer]] and has free places
            // to be populated from IO buffer
            val byteBuffer = {
              val previousBuffer = buffer.toByteBuffer
              val minBlocks = (previousBuffer.capacity() / blockSize) min 1
              val byteBuffer = ByteBuffer.allocate(minBlocks * blockSize)
              byteBuffer.put(previousBuffer)
            }
            connection.read(
              byteBuffer,
              readTimeout.length,
              readTimeout.unit,
              (),
              new CompletionHandler[Integer, Any] {
                override def completed(result: Integer, attachment: Any): Unit =
                  register {
                    if (result >= 0)
                      UIO.succeed(BitVector(byteBuffer))
                    else
                      ZIO.fail(ClientError.ClosedConnection)
                  }

                override def failed(exc: Throwable, attachment: Any): Unit =
                  register(exc match {
                    case _: ShutdownChannelGroupException =>
                      ZIO.fail(ClientError.ClosedConnection)

                    case _: ClosedChannelException =>
                      ZIO.fail(ClientError.ClosedConnection)

                    case _: InterruptedByTimeoutException =>
                      ZIO.fail(ClientError.ReadTimeout)

                    // unexpected error, no workaround is available
                    case e: Throwable => ZIO.die(e)
                  })
              }
            )
          }

      override def write(buffer: BitVector,
                         connection: ClientSocket): IO[ClientError, Unit] =
        ZIO
          .effectAsync { register =>
            val byteVector = buffer.toByteVector

            connection.write(
              byteVector.toByteBuffer,
              writeTimeout.length,
              writeTimeout.unit,
              (),
              new CompletionHandler[Integer, Any] {
                override def completed(result: Integer, attachment: Any): Unit =
                  register {
                    if (result >= 0)
                      if (result < byteVector.size)
                        write(byteVector.drop(result.toLong).toBitVector,
                              connection)
                      else UIO.succeed(())
                    else
                      ZIO.fail(ClientError.ClosedConnection)
                  }

                override def failed(exc: Throwable, attachment: Any): Unit =
                  register(exc match {
                    case _: ShutdownChannelGroupException =>
                      ZIO.fail(ClientError.ClosedConnection)

                    case _: ClosedChannelException =>
                      ZIO.fail(ClientError.ClosedConnection)

                    case _: InterruptedByTimeoutException =>
                      ZIO.fail(ClientError.WriteTimeout)

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
