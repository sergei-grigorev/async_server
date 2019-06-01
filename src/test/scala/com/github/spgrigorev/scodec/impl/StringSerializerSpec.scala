package com.github.spgrigorev.scodec.impl

import java.nio.ByteBuffer

import cats.kernel.Eq
import com.github.spgrigorev.scodec.algebras.Serializer
import com.github.spgrigorev.scodec.laws.SerializerTests
import org.scalatest.FunSuiteLike
import org.typelevel.discipline.scalatest.Discipline

class StringSerializerSpec extends Discipline with FunSuiteLike {

  implicit final val eqInstance
    : Eq[Either[Serializer.DeserializeError, String]] = Eq.fromUniversalEquals

  checkAll("Serializer",
           SerializerTests[ByteBuffer, String](
             new StringSerializer {}.serializer).algebra)

}
