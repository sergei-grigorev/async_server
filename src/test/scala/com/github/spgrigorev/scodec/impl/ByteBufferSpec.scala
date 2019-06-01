package com.github.spgrigorev.scodec.impl

import java.nio.ByteBuffer

import cats.Eq
import com.github.spgrigorev.scodec.ArbitraryInstances
import cats.instances.boolean._
import cats.instances.int._
import com.github.spgrigorev.scodec.data.Buffer
import com.github.spgrigorev.scodec.instances.byteBuffer._
import com.github.spgrigorev.scodec.laws.BufferTests
import eu.timepit.refined.types.numeric.NonNegInt
import org.scalatest.FunSuiteLike
import org.typelevel.discipline.scalatest.Discipline

class ByteBufferSpec extends Discipline with FunSuiteLike with ArbitraryInstances {

  implicit val nonNegEq: Eq[NonNegInt] = Eq.by(_.value)

  checkAll("Buffer", BufferTests[ByteBuffer](Buffer[ByteBuffer]).algebra)

}
