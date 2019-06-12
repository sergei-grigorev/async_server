package com.github.spgrigorev.scodec

import com.github.spgrigorev.scodec.arbitrary.{MtProtoInstances, TypedStringInstances}
import eu.timepit.refined.types.numeric.{NonNegInt, PosInt}
import org.scalacheck.{Arbitrary, Gen}
import scodec.bits.BitVector

trait ArbitraryInstances extends TypedStringInstances with MtProtoInstances {

  val nonNegIntGen: Gen[NonNegInt] =
    Gen.chooseNum(0, 512).map(NonNegInt.unsafeFrom)

  val posIntGen: Gen[PosInt] =
    nonNegIntGen
      .withFilter(_.value > 0)
      .map(nonNeg => PosInt.unsafeFrom(nonNeg.value))

  val byteGen: Gen[Byte] = Gen.chooseNum(0, 255).map(_.toByte)

  val byteBufferGen: Gen[BitVector] =
    for {
      size <- nonNegIntGen
      array <- Gen.listOfN(size.value, byteGen).map(_.toArray[Byte])
    } yield BitVector(array)

  implicit val arbNonNeg: Arbitrary[NonNegInt] = Arbitrary(nonNegIntGen)
  implicit val arbByteBuffer: Arbitrary[BitVector] = Arbitrary(byteBufferGen)
  implicit val arbPosInt: Arbitrary[PosInt] = Arbitrary(posIntGen)
}
