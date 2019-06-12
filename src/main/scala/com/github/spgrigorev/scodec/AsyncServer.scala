package com.github.spgrigorev.scodec

import cats.Show
import cats.instances.string._
import com.github.spgrigorev.scodec.algebras.{NetworkProtocol, Server}
import com.github.spgrigorev.scodec.domain.ServerError
import com.github.spgrigorev.scodec.instances.all._
import com.github.spgrigorev.scodec.model.Config
import scalaz.zio.ZIO

trait AsyncServer {
  import NetworkProtocol.algebra._
  import Server.algebra._
  import com.github.spgrigorev.scodec.algebras.Connection.algebra._
  import com.github.spgrigorev.scodec.algebras.Logger.algebra._

  /**
    * Start async server.
    *
    * @param config configuration
    * @tparam SERVER server type
    * @tparam CONNECTION connection type
    * @tparam MESSAGE deserialized message type
    * @return potentially indefinite value
    */
  def startServer[SERVER, CONNECTION: Show, MESSAGE](
      config: Config)
    : ZIO[NetworkProtocol.Environment[CONNECTION, MESSAGE] with Server[
            SERVER,
            CONNECTION],
          Nothing,
          Unit] = {

    ZIO
      .bracket(start[SERVER, CONNECTION](config) <*
        info("start server configuration: ", config))(server =>
        stop[SERVER, CONNECTION](server) <* info("stop server")) { server =>
        accept[SERVER, CONNECTION](server).interruptible.flatMap { c =>
          info("connected new client: ", c) *>
            registerClient[CONNECTION, MESSAGE](c)
              .ensuring(info("disconnected client: ", c) *> disconnect(c))
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
        case ServerError.ServerStopped =>
          info("server has stopped")
      }
  }
}
