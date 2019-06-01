package com.github.spgrigorev.scodec.impl

import com.github.spgrigorev.scodec.algebras.NetworkProtocol
import com.github.spgrigorev.scodec.basic.RequestResponseProtocol
import com.github.spgrigorev.scodec.domain.ClientError
import com.github.spgrigorev.scodec.impl.BasicBufferProtocol.ConnectionState
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.auto._
import scalaz.zio.{IO, Ref, UIO}

trait BasicBufferProtocol[BUFFER, MESSAGE, CONNECTION]
    extends NetworkProtocol[CONNECTION, BUFFER, MESSAGE] {

  override def protocol: NetworkProtocol.Service[CONNECTION, BUFFER, MESSAGE] =
    new RequestResponseProtocol[BUFFER,
                                MESSAGE,
                                ConnectionState[MESSAGE],
                                CONNECTION] {

      override def makeNewState(
          connection: CONNECTION): UIO[Ref[ConnectionState[MESSAGE]]] =
        Ref.make(ConnectionState(Option.empty[MESSAGE]))

      override def callback(message: MESSAGE,
                            state: Ref[ConnectionState[MESSAGE]])
        : IO[ClientError, List[MESSAGE]] = {
        for {
          prevState <- state.get
          _ <- state.update(_.copy(prevMessage = Some(message)))
          output <- UIO(prevState.prevMessage.toList)
        } yield output
      }

      override val initBufferSize: NonNegInt = 512
    }
}

object BasicBufferProtocol {
  case class ConnectionState[B](prevMessage: Option[B])
}
