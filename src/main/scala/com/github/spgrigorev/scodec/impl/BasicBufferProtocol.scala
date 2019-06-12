package com.github.spgrigorev.scodec.impl

import com.github.spgrigorev.scodec.algebras.NetworkProtocol
import com.github.spgrigorev.scodec.basic.RequestResponseProtocol
import com.github.spgrigorev.scodec.domain.ClientError
import com.github.spgrigorev.scodec.impl.BasicBufferProtocol.ConnectionState
import scalaz.zio.{IO, UIO}

trait BasicBufferProtocol[MESSAGE, CONNECTION]
    extends NetworkProtocol[CONNECTION, MESSAGE] {

  override def protocol: NetworkProtocol.Service[CONNECTION, MESSAGE] =
    new RequestResponseProtocol[MESSAGE, ConnectionState[MESSAGE], CONNECTION] {

      override def makeNewState(
          connection: CONNECTION): UIO[ConnectionState[MESSAGE]] =
        UIO(ConnectionState(Option.empty[MESSAGE]))

      override def callback(message: MESSAGE, state: ConnectionState[MESSAGE])
        : IO[ClientError, (List[MESSAGE], ConnectionState[MESSAGE])] = {
        val output = state.prevMessage.toList
        val newState = state.copy(prevMessage = Some(message))
        UIO((output, newState))
      }
    }
}

object BasicBufferProtocol {
  case class ConnectionState[B](prevMessage: Option[B])
}
