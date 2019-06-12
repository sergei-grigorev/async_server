package com.github.spgrigorev.scodec.basic

import cats.instances.list._
import cats.instances.string._
import cats.kernel.laws._
import cats.kernel.laws.discipline._
import com.github.spgrigorev.scodec.ArbitraryInstances
import com.github.spgrigorev.scodec.basic.RequestResponseProtocolSpecTemplate.{
  TestEnvironment,
  TypedTest
}
import com.github.spgrigorev.scodec.domain.ClientError
import com.github.spgrigorev.scodec.impl.CStringSerializer
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.auto._
import org.scalacheck.Prop
import org.scalatest.FunSuiteLike
import org.scalatestplus.scalacheck.Checkers
import scalaz.zio._

class CStringRequestResponseProtocolSpec
    extends RequestResponseProtocolTemplate[String]
    with FunSuiteLike
    with Checkers
    with ArbitraryInstances {

  override def environment(
      ref: Ref[RequestResponseProtocolSpecTemplate.TestBufferState])
    : protocolToTest.FixedEnvironment =
    new TestEnvironment(ref) with CStringSerializer {
      override val readBytesBucketSize: NonNegInt = 4
    }

  /**
    * our test object duplicates every input message.
    */
  override val protocolToTest: RequestResponseProtocol[String, Unit, Unit] =
    new RequestResponseProtocol[String, Unit, Unit] {
      override def makeNewState(connection: Unit): UIO[Unit] = UIO.unit

      override def callback(
          messages: String,
          state: Unit): IO[ClientError, (List[String], Unit)] =
        UIO(messages :: messages :: Nil, ())
    }

  test(
    "tested request-response protocol always returns duplicated input message") {
    check(
      Prop.forAllNoShrink { a: (List[String], TypedTest[String]) =>
        a match {
          case (orig, state) =>
            val result = unsafeRun(runZioTest(state))
            val expected = orig.flatMap(el => el :: el :: Nil)
            result <-> expected
        }
      }
    )
  }
}
