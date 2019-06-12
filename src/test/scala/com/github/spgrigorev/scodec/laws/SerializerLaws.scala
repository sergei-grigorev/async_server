package com.github.spgrigorev.scodec.laws

import cats.kernel.laws._
import cats.syntax.either._
import com.github.spgrigorev.scodec.algebras.Serializer

trait SerializerLaws[A] {
  def algebra: Serializer.Service[A]

  def deserializeSerialized(
      content: A): IsEq[Either[Serializer.SerializeError, A]] = {
    val deserialized = algebra
      .serialize(content)
      .flatMap(serialized => algebra.deserialize(serialized))
      .map(_._2)
    deserialized <-> content.asRight[Serializer.SerializeError]
  }

  def deserializedOnlyOnce(
      content: A): IsEq[Either[Serializer.SerializeError, A]] = {
    val twiceDeserialized =
      for {
        serialized <- algebra.serialize(content)
        first <- algebra.deserialize(serialized)
        (remainer, _) = first
        second <- algebra.deserialize(remainer)
        (_, result) = second
      } yield result
    twiceDeserialized <-> Serializer.SerializeError.notEnough().asLeft[A]
  }
}

object SerializerLaws {
  def apply[A](instance: Serializer.Service[A]): SerializerLaws[A] =
    new SerializerLaws[A] {
      override def algebra: Serializer.Service[A] = instance
    }
}
