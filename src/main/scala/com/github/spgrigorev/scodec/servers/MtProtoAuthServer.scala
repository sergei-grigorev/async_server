package com.github.spgrigorev.scodec.servers

import com.github.spgrigorev.scodec.AsyncServer
import com.github.spgrigorev.scodec.impl._
import com.github.spgrigorev.scodec.instances.clientChannel._
import com.github.spgrigorev.scodec.model.{Config, MtProtoMessage}
import eu.timepit.refined.auto._
import eu.timepit.refined.types.net.PortNumber
import scalaz.zio.{App, ZIO}

object MtProtoAuthServer extends App with AsyncServer {

  override def run(args: List[String]): ZIO[Environment, Nothing, Int] = {
    val portNumber: PortNumber = 9191
    startServer[AsyncServerSocket.ServerSocket,
                AsyncServerSocket.ClientSocket,
                MtProtoMessage](Config(port = portNumber, "localhost"))
      .map(_ => 0)
      .provide(
        new AsyncServerSocket with AsyncConnection with ConsoleLogger
        with MtProtoSerializer
        with MtProtoAuthProtocol[AsyncConnection.ClientSocket]
      )
  }
}
