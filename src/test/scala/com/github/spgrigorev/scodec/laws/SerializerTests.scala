package com.github.spgrigorev.scodec.laws

import cats.Eq
import cats.kernel.laws.discipline._
import com.github.spgrigorev.scodec.algebras.Serializer
import org.scalacheck.Arbitrary
import org.scalacheck.Prop._
import org.typelevel.discipline.Laws

trait SerializerTests[A] extends Laws {
  def laws: SerializerLaws[A]

  def algebra(implicit arbContent: Arbitrary[A],
              eqOptContentA: Eq[Either[Serializer.SerializeError, A]]) =
    new SimpleRuleSet(
      name = "Serializer",
      "deserialized is equal original" -> forAll(laws.deserializeSerialized _),
      "deserialized only once" -> forAll(laws.deserializedOnlyOnce _)
    )
}

object SerializerTests {
  def apply[A](instance: Serializer.Service[A]): SerializerTests[A] =
    new SerializerTests[A] {
      override val laws: SerializerLaws[A] = SerializerLaws(instance)
    }
}
