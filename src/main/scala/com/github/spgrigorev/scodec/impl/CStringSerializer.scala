package com.github.spgrigorev.scodec.impl

import cats.syntax.either._
import com.github.spgrigorev.scodec.algebras.Serializer
import com.github.spgrigorev.scodec.algebras.Serializer.SerializeError
import scodec.bits.BitVector
import scodec.codecs._

/**
  * Implementation for zero-terminated string.
  */
trait CStringSerializer extends Serializer[String] {
  override val serializer: Serializer.Service[String] =
    new Serializer.Service[String] {

      /**
        * Find all available messages in a buffer.
        */
      override def deserialize(buffer: BitVector)
        : Either[Serializer.SerializeError, (BitVector, String)] = {

        cstring
          .decode(buffer)
          .toEither
          .bimap({ _ =>
            SerializeError.notEnough()
          }, { decoded =>
            (decoded.remainder, decoded.value)
          })
      }

      override def serialize(
          message: String): Either[SerializeError, BitVector] =
        cstring.encode(message).require.asRight[SerializeError]
    }
}
