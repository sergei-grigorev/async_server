package com.github.spgrigorev.scodec.algebras

import cats.Show
import com.github.spgrigorev.scodec.domain.ClientError
import scalaz.zio.ZIO

import scala.language.higherKinds

/**
  * Handshake protocol.
  *
  * @tparam CONNECTION connection type
  * @tparam MESSAGE user message type
  */
trait NetworkProtocol[CONNECTION, MESSAGE] {
  def protocol: NetworkProtocol.Service[CONNECTION, MESSAGE]
}

object NetworkProtocol {
  type Environment[CONNECTION, MESSAGE] =
    NetworkProtocol[CONNECTION, MESSAGE]
      with Connection[CONNECTION]
      with Serializer[MESSAGE]
      with Logger

  type ServiceEnvironment[CONNECTION, MESSAGE] =
    Connection[CONNECTION] with Serializer[MESSAGE] with Logger

  trait Service[CONNECTION, MESSAGE] {
    def registerClient(connection: CONNECTION)(
        implicit connectionShow: Show[CONNECTION])
      : ZIO[ServiceEnvironment[CONNECTION, MESSAGE], ClientError, Unit]
  }

  object algebra {
    def registerClient[CONNECTION: Show, MESSAGE](connection: CONNECTION)
      : ZIO[Environment[CONNECTION, MESSAGE], ClientError, Unit] =
      ZIO.accessM(_.protocol.registerClient(connection))
  }
}
