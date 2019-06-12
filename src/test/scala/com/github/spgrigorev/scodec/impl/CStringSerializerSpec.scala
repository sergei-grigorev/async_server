package com.github.spgrigorev.scodec.impl

import cats.kernel.Eq
import com.github.spgrigorev.scodec.ArbitraryInstances
import com.github.spgrigorev.scodec.algebras.Serializer
import com.github.spgrigorev.scodec.laws.SerializerTests
import org.scalatest.FunSuiteLike
import org.typelevel.discipline.scalatest.Discipline

class CStringSerializerSpec
    extends Discipline
    with FunSuiteLike
    with ArbitraryInstances {

  implicit final val eqInstance: Eq[Either[Serializer.SerializeError, String]] =
    Eq.fromUniversalEquals

  checkAll("CStringSerializer",
           SerializerTests[String](new CStringSerializer {}.serializer).algebra)

}
