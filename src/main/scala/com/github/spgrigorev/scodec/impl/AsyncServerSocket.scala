package com.github.spgrigorev.scodec.impl

import java.net.InetSocketAddress
import java.nio.channels._

import com.github.spgrigorev.scodec.algebras.{Logger, Server}
import com.github.spgrigorev.scodec.domain.ServerError
import com.github.spgrigorev.scodec.impl.AsyncServerSocket.{ClientSocket, ServerSocket}
import com.github.spgrigorev.scodec.instances.throwable._
import com.github.spgrigorev.scodec.model.Config
import scalaz.zio.{IO, UIO, ZIO}

trait AsyncServerSocket extends Server[ServerSocket, ClientSocket] {
  override val server: Server.Service[ServerSocket, ClientSocket] =
    new Server.Service[ServerSocket, ClientSocket] {

      /**
        * Start server.
        *
        * @param config config with port number
        * @return link to the server status
        */
      override def start(config: Config): IO[ServerError, ServerSocket] = {
        val server =
          for {
            server <- IO(AsynchronousServerSocketChannel.open())
            _ <- IO(
              server.bind(
                new InetSocketAddress(config.interface, config.port.value)))
          } yield server

        server.refineOrDie {
          case _: AlreadyBoundException => ServerError.PortIsNotAvailable
          case e: UnsupportedAddressTypeException =>
            ServerError.InterfaceError(e)
        }
      }

      /**
        * Stop server. Used to release resources.
        */
      override def stop(
          server: ServerSocket): ZIO[Logger, Nothing, Unit] =
        IO(server.close()).catchAll { e =>
          Logger.algebra.error("server stopped with an error", e)
        }

      /**
        * Await to get a new connection.
        */
      override def accept(
          server: ServerSocket): IO[ServerError, ClientSocket] = {
        ZIO.effectAsync { register =>
          server.accept(
            (),
            new CompletionHandler[AsynchronousSocketChannel, Any] {
              override def completed(result: ClientSocket,
                                     attachment: Any): Unit =
                register(UIO(result))

              override def failed(exc: Throwable, attachment: Any): Unit =
                register(exc match {
                  case _: ShutdownChannelGroupException =>
                    ZIO.fail(ServerError.ServerStopped)

                  // unexpected error, no workaround is available
                  case e: Throwable => ZIO.die(e)
                })
            }
          )
        }
      }
    }
}

object AsyncServerSocket {
  type ServerSocket = AsynchronousServerSocketChannel
  type ClientSocket = AsynchronousSocketChannel
}
