package com.github.spgrigorev.scodec.algebras

import cats.data.NonEmptyList
import scalaz.zio.ZIO

/**
  * Serialization service.
  *
  * @tparam BUFFER IO buffer
  * @tparam MESSAGE message type
  */
trait Serializer[BUFFER, MESSAGE] {
  def serializer: Serializer.Service[BUFFER, MESSAGE]
}

object Serializer {
  trait Service[BUFFER, MESSAGE] {
    def deserialize(
        buffer: BUFFER): Either[DeserializeError, MessageState[BUFFER, MESSAGE]]
    def serialize(message: MESSAGE): BUFFER
  }

  object algebra {
    def serialize[BUFFER, MESSAGE](
        message: BUFFER): ZIO[Serializer[MESSAGE, BUFFER], Nothing, MESSAGE] =
      ZIO.access[Serializer[MESSAGE, BUFFER]](_.serializer.serialize(message))

    def deserialize[MESSAGE, BUFFER](buffer: MESSAGE)
      : ZIO[Serializer[MESSAGE, BUFFER],
            Nothing,
            Either[DeserializeError, MessageState[MESSAGE, BUFFER]]] =
      ZIO.access(_.serializer.deserialize(buffer))
  }

  type MessageState[BUFFER, MESSAGE] = (BUFFER, NonEmptyList[MESSAGE])

  sealed trait DeserializeError extends Product with Serializable
  object DeserializeError {
    case object NotEnough extends DeserializeError
    case class WrongContent(message: String) extends DeserializeError

    def notEnough(): DeserializeError = NotEnough
    def wrongContent(message: String): DeserializeError = WrongContent(message)
  }
}
