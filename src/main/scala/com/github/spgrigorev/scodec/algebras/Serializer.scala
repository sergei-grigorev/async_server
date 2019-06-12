package com.github.spgrigorev.scodec.algebras

import scalaz.zio.ZIO
import scodec.bits.BitVector

/**
  * Serialization service.
  *
  * @tparam MESSAGE message type
  */
trait Serializer[MESSAGE] {
  def serializer: Serializer.Service[MESSAGE]
}

object Serializer {
  trait Service[MESSAGE] {
    def deserialize(
        buffer: BitVector): Either[SerializeError, MessageState[MESSAGE]]

    def serialize(message: MESSAGE): Either[SerializeError, BitVector]
  }

  object algebra {
    def serialize[MESSAGE](
        message: MESSAGE): ZIO[Serializer[MESSAGE], SerializeError, BitVector] =
      ZIO.accessM[Serializer[MESSAGE]](c =>
        ZIO.fromEither(c.serializer.serialize(message)))

    def deserialize[MESSAGE](buffer: BitVector)
      : ZIO[Serializer[MESSAGE], SerializeError, MessageState[MESSAGE]] =
      ZIO.accessM(c => ZIO.fromEither(c.serializer.deserialize(buffer)))
  }

  type MessageState[MESSAGE] = (BitVector, MESSAGE)

  sealed trait SerializeError extends Product with Serializable
  object SerializeError {
    case object NotEnough extends SerializeError
    case class WrongContent(error: String) extends SerializeError

    def notEnough(): SerializeError = NotEnough
    def wrongContent(message: String): SerializeError = WrongContent(message)
  }
}
