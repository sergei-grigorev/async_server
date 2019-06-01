package com.github.spgrigorev.scodec.algebras

import cats.data.NonEmptyList
import scalaz.zio.ZIO

/**
  * Serialization service.
  *
  * @tparam A IO buffer
  * @tparam B message type
  */
trait Serializer[A, B] {
  def serializer: Serializer.Service[A, B]
}

object Serializer {
  trait Service[A, B] {
    def deserialize(buffer: A): Either[DeserializeError, MessageState[A, B]]
    def serialize(message: B): A
  }

  object algebra {
    def serialize[B, A](message: B): ZIO[Serializer[A, B], Nothing, A] =
      ZIO.access[Serializer[A, B]](_.serializer.serialize(message))

    def deserialize[A, B](
        buffer: A): ZIO[Serializer[A, B],
                        Nothing,
                        Either[DeserializeError, MessageState[A, B]]] =
      ZIO.access(_.serializer.deserialize(buffer))
  }

  type MessageState[A, B] = (A, NonEmptyList[B])

  sealed trait DeserializeError extends Product with Serializable
  object DeserializeError {
    case object NotEnough extends DeserializeError
    case class WrongContent(message: String) extends DeserializeError

    def notEnough(): DeserializeError = NotEnough
    def wrongContent(message: String): DeserializeError = WrongContent(message)
  }
}
