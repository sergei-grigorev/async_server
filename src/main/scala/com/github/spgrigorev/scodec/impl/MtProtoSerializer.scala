package com.github.spgrigorev.scodec.impl

import cats.syntax.either._
import com.github.spgrigorev.scodec.algebras.Serializer
import com.github.spgrigorev.scodec.algebras.Serializer.SerializeError
import com.github.spgrigorev.scodec.model.MtProtoMessage
import scodec.bits.BitVector
import scodec.{Codec, Err}
import com.github.spgrigorev.scodec.codecs.MtProtoCodec._

trait MtProtoSerializer extends Serializer[MtProtoMessage] {
  override val serializer: Serializer.Service[MtProtoMessage] =
    new Serializer.Service[MtProtoMessage] {

      override def deserialize(
          buffer: BitVector): Either[SerializeError, (BitVector, MtProtoMessage)] = {
        Codec[MtProtoMessage]
          .decode(buffer)
          .toEither
          .bimap(
            {
              case Err.InsufficientBits(_, _, _) =>
                SerializeError.notEnough()
              case e =>
                SerializeError
                  .wrongContent(
                    s"problem with decoding: ${e.messageWithContext}")
            }, { decoded =>
              (decoded.remainder, decoded.value)
            }
          )
      }

      override def serialize(
          message: MtProtoMessage): Either[SerializeError, BitVector] = {
        Codec[MtProtoMessage]
          .encode(message)
          .toEither
          .bimap(
            {
              case Err.InsufficientBits(_, _, _) =>
                SerializeError.notEnough()

              case e =>
                SerializeError
                  .wrongContent(
                    s"problem with encode $message: ${e.messageWithContext}")
            },
            identity
          )
      }
    }
}
