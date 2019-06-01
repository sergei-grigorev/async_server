package com.github.spgrigorev.scodec.laws

import cats.Eq
import cats.kernel.laws.discipline._
import com.github.spgrigorev.scodec.algebras.Serializer
import org.scalacheck.Arbitrary
import org.scalacheck.Prop._
import org.typelevel.discipline.Laws

trait SerializerTests[A, B] extends Laws {
  def laws: SerializerLaws[A, B]

  def algebra(implicit arbContent: Arbitrary[B],
              eqOptContentA: Eq[Either[Serializer.DeserializeError, B]]) =
    new SimpleRuleSet(
      name = "Serializer",
      "deserialized is equal original" -> forAll(laws.deserializeSerialized _),
      "deserialized only once" -> forAll(laws.deserializedOnlyOnce _)
    )
}

object SerializerTests {
  def apply[A, B](instance: Serializer.Service[A, B]): SerializerTests[A, B] =
    new SerializerTests[A, B] {
      override val laws: SerializerLaws[A, B] = SerializerLaws(instance)
    }
}
