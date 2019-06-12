package com.github.spgrigorev.scodec.impl

import cats.kernel.Eq
import com.github.spgrigorev.scodec.ArbitraryInstances
import com.github.spgrigorev.scodec.algebras.Serializer
import com.github.spgrigorev.scodec.laws.SerializerTests
import com.github.spgrigorev.scodec.model.MtProtoMessage
import org.scalatest.FunSuiteLike
import org.typelevel.discipline.scalatest.Discipline

class MtProtoSerializerSpec
    extends Discipline
    with FunSuiteLike
    with ArbitraryInstances {

  implicit final val eqInstance: Eq[Either[Serializer.SerializeError, MtProtoMessage]] =
    Eq.fromUniversalEquals

  checkAll("MtProtoSerializer",
           SerializerTests[MtProtoMessage](new MtProtoSerializer {}.serializer).algebra)

}
