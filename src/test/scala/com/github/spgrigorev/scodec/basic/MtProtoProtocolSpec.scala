package com.github.spgrigorev.scodec.basic

import cats.kernel.Eq
import cats.kernel.laws._
import cats.kernel.laws.discipline._
import cats.instances.list._
import com.github.spgrigorev.scodec.ArbitraryInstances
import com.github.spgrigorev.scodec.algebras.NetworkProtocol
import com.github.spgrigorev.scodec.algebras.NetworkProtocol.ServiceEnvironment
import com.github.spgrigorev.scodec.basic.RequestResponseProtocolSpecTemplate.{TestEnvironment, TypedTest}
import com.github.spgrigorev.scodec.codecs.BigNumeric
import com.github.spgrigorev.scodec.codecs.BigNumeric.Int128
import com.github.spgrigorev.scodec.impl.{MtProtoAuthProtocol, MtProtoSerializer}
import com.github.spgrigorev.scodec.model.MtProto.ResPQ
import com.github.spgrigorev.scodec.model.{MtProto, MtProtoMessage}
import eu.timepit.refined.auto._
import eu.timepit.refined.types.numeric.NonNegInt
import org.scalacheck.Prop
import org.scalatest.FunSuiteLike
import org.scalatestplus.scalacheck.Checkers
import scalaz.zio._

class MtProtoProtocolSpec
    extends RequestResponseProtocolTemplate[MtProtoMessage]
    with FunSuiteLike
    with Checkers
    with ArbitraryInstances {

  implicit final val eqInstance: Eq[MtProtoMessage] =
    Eq.fromUniversalEquals

  override val protocolToTest: NetworkProtocol.Service[Unit, MtProtoMessage] =
    new MtProtoAuthProtocol[Unit] {
      override def serverNonceGen: IO[Nothing, BigNumeric.Int128] =
        UIO.succeed(BigNumeric.Int128(0L, 0L))

      override def pqGen: IO[Nothing, String] = UIO("0")

      override def fingerPrintsGen: IO[Nothing, ResPQ.Fingerprints] =
        UIO.succeed(ResPQ.Fingerprints(List(0L)))

      override def encryptedAnswerGen(
          p: String,
          q: String,
          publicKeyFingerPrint: FiberId,
          encryptedData: String): IO[Nothing, String] =
        UIO.succeed("0" * 596)
    }.protocol

  override def environment(
      ref: Ref[RequestResponseProtocolSpecTemplate.TestBufferState])
    : ServiceEnvironment[Unit, MtProtoMessage] =
    new TestEnvironment(ref) with MtProtoSerializer {
      override val readBytesBucketSize: NonNegInt = 128
    }

  test("always successfully starts session") {
    check(
      Prop.forAllNoShrink { a: (Int128, TypedTest[MtProtoMessage]) =>
        a match {
          case (nonce, state) =>
            val result = unsafeRun(runZioTest(state))

            val expected0 = MtProtoMessage(
              messageId = 1,
              message = MtProto.ResPQ(
                nonce = nonce,
                serverNonce = BigNumeric.Int128(0L, 0L),
                pq = "0",
                serverPublicKey = ResPQ.Fingerprints(List(0L))
              )
            )

            val expected1 = MtProtoMessage(
              // starts from 0
              messageId = 3,
              message = MtProto.ServerDHParamsOk(
                nonce = nonce,
                serverNonce = BigNumeric.Int128(0L, 0L),
                encryptedAnswer = "0" * 596
              )
            )

            result <-> List(expected0, expected1)
        }
      }
    )
  }

}
