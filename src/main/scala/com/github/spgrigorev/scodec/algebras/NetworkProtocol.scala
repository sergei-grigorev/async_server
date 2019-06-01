package com.github.spgrigorev.scodec.algebras

import cats.Show
import com.github.spgrigorev.scodec.domain.ClientError
import scalaz.zio.ZIO

import scala.language.higherKinds

/**
  * Handshake protocol.
  *
  * @tparam C connection type
  * @tparam A connection buffer type
  * @tparam B user message type
  */
trait NetworkProtocol[C, A, B] {
  def protocol: NetworkProtocol.Service[C, A, B]
}

object NetworkProtocol {
  type Environment[C, A, B] = NetworkProtocol[C, A, B]
    with Connection[C, A]
    with Serializer[A, B]
    with Logger

  type ServiceEnvironment[C, A, B] =
    Connection[C, A] with Serializer[A, B] with Logger

  trait Service[C, A, B] {
    def registerClient(
        connection: C): ZIO[ServiceEnvironment[C, A, B], ClientError, Unit]
  }

  object algebra {
    def registerClient[C: Show, A, B](
        connection: C): ZIO[Environment[C, A, B], ClientError, Unit] =
      ZIO.accessM(_.protocol.registerClient(connection))
  }
}
