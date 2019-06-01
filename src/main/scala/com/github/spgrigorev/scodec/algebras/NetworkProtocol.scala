package com.github.spgrigorev.scodec.algebras

import cats.Show
import com.github.spgrigorev.scodec.data.Buffer
import com.github.spgrigorev.scodec.domain.ClientError
import scalaz.zio.ZIO

import scala.language.higherKinds

/**
  * Handshake protocol.
  *
  * @tparam CONNECTION connection type
  * @tparam BUFFER connection buffer type
  * @tparam MESSAGE user message type
  */
trait NetworkProtocol[CONNECTION, BUFFER, MESSAGE] {
  def protocol: NetworkProtocol.Service[CONNECTION, BUFFER, MESSAGE]
}

object NetworkProtocol {
  type Environment[CONNECTION, BUFFER, MESSAGE] =
    NetworkProtocol[CONNECTION, BUFFER, MESSAGE]
      with Connection[CONNECTION, BUFFER]
      with Serializer[BUFFER, MESSAGE]
      with Logger

  type ServiceEnvironment[CONNECTION, BUFFER, MESSAGE] =
    Connection[CONNECTION, BUFFER] with Serializer[BUFFER, MESSAGE] with Logger

  trait Service[CONNECTION, BUFFER, MESSAGE] {
    def registerClient(connection: CONNECTION)(implicit BUFFER: Buffer[BUFFER])
      : ZIO[ServiceEnvironment[CONNECTION, BUFFER, MESSAGE], ClientError, Unit]
  }

  object algebra {
    def registerClient[CONNECTION: Show, BUFFER: Buffer, MESSAGE](
        connection: CONNECTION)
      : ZIO[Environment[CONNECTION, BUFFER, MESSAGE], ClientError, Unit] =
      ZIO.accessM(_.protocol.registerClient(connection))
  }
}
