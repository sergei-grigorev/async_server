package com.github.spgrigorev.scodec.laws

import com.github.spgrigorev.scodec.algebras.Serializer
import cats.kernel.laws._
import cats.syntax.either._

trait SerializerLaws[A, B] {
  def algebra: Serializer.Service[A, B]

  def deserializeSerialized(
      content: B): IsEq[Either[Serializer.DeserializeError, B]] = {
    val serialized = algebra.serialize(content)
    val deserialized = algebra.deserialize(serialized).map(_._2.head)
    deserialized <-> content.asRight[Serializer.DeserializeError]
  }

  def deserializedOnlyOnce(
      content: B): IsEq[Either[Serializer.DeserializeError, B]] = {
    val serialized = algebra.serialize(content)
    val secondTime =
      for {
        updatedBuffer <- algebra.deserialize(serialized).map(_._1)
        secondTime <- algebra.deserialize(updatedBuffer)
      } yield secondTime._2.head
    secondTime <-> Serializer.DeserializeError.notEnough().asLeft[B]
  }
}

object SerializerLaws {
  def apply[A, B](instance: Serializer.Service[A, B]): SerializerLaws[A, B] =
    new SerializerLaws[A, B] {
      override def algebra: Serializer.Service[A, B] = instance
    }
}
