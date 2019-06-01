package com.github.spgrigorev.scodec.algebras

import com.github.spgrigorev.scodec.domain.ServerError
import com.github.spgrigorev.scodec.model.Config
import scalaz.zio.{IO, UIO, ZIO}

import scala.language.higherKinds

/**
  * Server module.
  * @tparam SERVER type of server
  * @tparam CONNECTION type of connection
  */
trait Server[SERVER, CONNECTION] {
  def server: Server.Service[SERVER, CONNECTION]
}

object Server {

  trait Service[SERVER, CONNECTION] {

    /**
      * Start server.
      *
      * @param config config with port number
      * @return link to the server status
      */
    def start(config: Config): IO[ServerError, SERVER]

    /**
      * Stop server. Used to release resources.
      */
    def stop(server: SERVER): ZIO[Logger, Nothing, Unit]

    /**
      * Await to get a new connection.
      */
    def accept(server: SERVER): IO[ServerError, CONNECTION]
  }

  object algebra {
    def start[SERVER, CONNECTION](
        config: Config): ZIO[Server[SERVER, CONNECTION], ServerError, SERVER] =
      ZIO.accessM(_.server.start(config))

    def accept[SERVER, CONNECTION](server: SERVER)
      : ZIO[Server[SERVER, CONNECTION], ServerError, CONNECTION] =
      ZIO.accessM(_.server.accept(server))

    def stop[SERVER, CONNECTION](server: SERVER)
      : ZIO[Server[SERVER, CONNECTION] with Logger, Nothing, Unit] =
      ZIO.accessM(_.server.stop(server))
  }
}
