package com.github.spgrigorev.scodec.algebras

import com.github.spgrigorev.scodec.domain.ServerError
import com.github.spgrigorev.scodec.model.Config
import scalaz.zio.{IO, UIO, ZIO}

import scala.language.higherKinds

/**
  * Server module.
  * @tparam S type of server
  * @tparam C type of connection
  */
trait Server[S, C] {
  def server: Server.Service[S, C]
}

object Server {

  trait Service[S, C] {

    /**
      * Start server.
      *
      * @param config config with port number
      * @return link to the server status
      */
    def start(config: Config): IO[ServerError, S]

    /**
      * Stop server. Used to release resources.
      */
    def stop(server: S): ZIO[Logger, Nothing, Unit]

    /**
      * Await to get a new connection.
      */
    def accept(server: S): IO[ServerError, C]
  }

  object algebra {
    def start[S, C](config: Config): ZIO[Server[S, C], ServerError, S] =
      ZIO.accessM(_.server.start(config))

    def accept[S, C](server: S): ZIO[Server[S, C], ServerError, C] =
      ZIO.accessM(_.server.accept(server))

    def stop[S, C](server: S): ZIO[Server[S, C] with Logger, Nothing, Unit] =
      ZIO.accessM(_.server.stop(server))
  }
}
