package com.github.spgrigorev.scodec

import cats.Show
import cats.instances.string._
import com.github.spgrigorev.scodec.algebras.{NetworkProtocol, Server}
import com.github.spgrigorev.scodec.domain.ServerError
import com.github.spgrigorev.scodec.model.Config
import scalaz.zio.{App, ZIO}
import instances.all._

object AsyncServer extends App {
  import NetworkProtocol.algebra._
  import Server.algebra._
  import com.github.spgrigorev.scodec.algebras.Connection.algebra._
  import com.github.spgrigorev.scodec.algebras.Logger.algebra._

  override def run(
      args: List[String]): ZIO[AsyncServer.Environment, Nothing, Int] = ???

  /**
    * Start async server.
    *
    * @param config configuration
    * @tparam S server type
    * @tparam C connection type
    * @tparam A connection buffer type
    * @tparam B deserialized message type
    * @return potentially indefinite value
    */
  def startServer[S, C: Show, A, B](config: Config)
    : ZIO[NetworkProtocol.Environment[C, A, B] with Server[S, C],
          Nothing,
          Unit] = {

    ZIO
      .bracket(
        start[S, C](config) <* info("start server configuration: ", config))(
        server => stop[S, C](server) <* info("stop server")) { server =>
        accept[S, C](server).interruptible.flatMap { c =>
          info("connected new client: ", c) *> registerClient[C, A, B](c)
            .ensuringR(info("disconnected client: ", c) *> disconnect(c))
            .supervised
            .fork
        }.forever
      }
      .catchAll {
        case ServerError.PortIsNotAvailable =>
          error("port in not available")
        case ServerError.ConfigurationError(errors) =>
          error("configuration error: ", errors)
        case ServerError.InterfaceError(cause) =>
          error("interface error: ", cause)
      }
  }
}
