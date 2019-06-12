package com.github.spgrigorev.scodec.arbitrary

import cats.MonoidK
import cats.syntax.semigroupk._
import cats.syntax.semigroup._
import cats.instances.list._
import com.github.spgrigorev.scodec.basic.RequestResponseProtocolSpecTemplate.TypedTest
import org.scalacheck.{Arbitrary, Gen}
import scodec.bits.BitVector
import scodec.codecs.cstring

trait TypedStringInstances {
  val asciiString: Gen[String] = Gen.asciiPrintableStr

  val testBufferStateGen: Gen[(String, List[BitVector])] = for {
    msg <- asciiString
    array = cstring.encode(msg).require
  } yield (msg, List(array))

  val testBufferSplitStateGen: Gen[(String, List[BitVector])] = for {
    msg <- asciiString
    splitAt <- Gen.sized(length => Gen.choose(0, length))
    (firstPart, secondPart) = msg.splitAt(splitAt)
    // remove last '\0' byte (8 bits) from the first array
    arrays = cstring.encode(firstPart).require.dropRight(8L) ::
      cstring.encode(secondPart).require ::
      Nil
  } yield (msg, arrays)

  val testBufferEmptyGen: Gen[(List[String], TypedTest[String])] =
    Gen.const((List.empty, MonoidK[TypedTest].empty[String]))

  val testBuffer1MessageStateGen: Gen[(List[String], TypedTest[String])] = for {
    word <- Gen.oneOf(testBufferStateGen, testBufferSplitStateGen)
    (msg, arrays) = word
  } yield (List(msg), TypedTest[String](arrays))

  val testBuffer2MessagesStateGen: Gen[(List[String], TypedTest[String])] =
    for {
      word1 <- testBuffer1MessageStateGen
      (msg1, arrays1) = word1
      word2 <- testBuffer1MessageStateGen
      (msg2, arrays2) = word2
    } yield (msg1 |+| msg2, arrays1 <+> arrays2)

  implicit val arbTestBufferState
    : Arbitrary[(List[String], TypedTest[String])] =
    Arbitrary(
      Gen.oneOf(testBufferEmptyGen,
                testBuffer1MessageStateGen,
                testBuffer2MessagesStateGen))

  implicit val arbAsciiString: Arbitrary[String] = Arbitrary(asciiString)
}
