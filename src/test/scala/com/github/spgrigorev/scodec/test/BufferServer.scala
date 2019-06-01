package com.github.spgrigorev.scodec.test

import java.nio.ByteBuffer

import com.github.spgrigorev.scodec.AsyncServer
import com.github.spgrigorev.scodec.impl.{
  AsyncConnection,
  AsyncServerSocket,
  BasicBufferProtocol,
  ConsoleLogger,
  StringSerializer
}
import com.github.spgrigorev.scodec.instances.clientChannel._
import com.github.spgrigorev.scodec.model.Config
import eu.timepit.refined.auto._
import eu.timepit.refined.types.net.PortNumber
import scalaz.zio.{App, ZIO}

object BufferServer extends App {

  override def run(
      args: List[String]): ZIO[AsyncServer.Environment, Nothing, Int] = {
    val portNumber: PortNumber = 9191
    AsyncServer
      .startServer[AsyncServerSocket.ServerSocket,
                   AsyncServerSocket.ClientSocket,
                   ByteBuffer,
                   String](Config(port = portNumber, "localhost"))
      .map(_ => 0)
      .provide(
        new AsyncServerSocket with AsyncConnection with ConsoleLogger
        with StringSerializer
        with BasicBufferProtocol[String, AsyncConnection.ClientSocket]
      )
  }
}
