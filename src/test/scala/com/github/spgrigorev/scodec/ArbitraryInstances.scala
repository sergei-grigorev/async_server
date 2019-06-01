package com.github.spgrigorev.scodec

import java.nio.ByteBuffer
import java.nio.charset.Charset

import com.github.spgrigorev.scodec.basic.RequestResponseProtocolSpec.TestBufferState
import eu.timepit.refined.types.numeric.{NonNegInt, PosInt}
import org.scalacheck.{Arbitrary, Gen}

trait ArbitraryInstances {
  val nonNegIntGen: Gen[NonNegInt] =
    Gen.chooseNum(0, 512).map(NonNegInt.unsafeFrom)

  val posIntGen: Gen[PosInt] =
    nonNegIntGen
      .withFilter(_.value > 0)
      .map(nonNeg => PosInt.unsafeFrom(nonNeg.value))

  val byteGen: Gen[Byte] = Gen.chooseNum(0, 255).map(_.toByte)

  val byteBufferGen: Gen[ByteBuffer] =
    for {
      size <- nonNegIntGen
      buffer <- Gen.listOfN(size.value, byteGen).map(_.toArray[Byte])
    } yield ByteBuffer.allocate(size.value).put(buffer)

  val charset: Charset = Charset.forName("UTF-8")

  val testBufferEmptyGen: Gen[(List[String], TestBufferState)] =
    Gen.const((List.empty, TestBufferState(List.empty)))

  val testBufferStateGen: Gen[(String, List[ByteBuffer])] = for {
    msg <- Gen.alphaLowerStr
    splitAt <- Gen.choose(0, msg.length)
    (firstPart, secondPart) = msg.splitAt(splitAt)
    arrays = charset.encode(firstPart) :: charset.encode(secondPart + "\0") :: Nil
  } yield (msg, arrays)

  val testBuffer1MessageStateGen: Gen[(List[String], TestBufferState)] = for {
    word <- testBufferStateGen
    (msg, arrays) = word
  } yield (List(msg), TestBufferState(arrays))

  val testBuffer2MessagesStateGen: Gen[(List[String], TestBufferState)] = for {
    word1 <- testBufferStateGen
    (msg1, arrays1) = word1
    word2 <- testBufferStateGen
    (msg2, arrays2) = word2
  } yield
    (msg1 :: msg2 :: Nil, TestBufferState((arrays1 :: arrays2 :: Nil).flatten))

  implicit val arbNonNeg: Arbitrary[NonNegInt] = Arbitrary(nonNegIntGen)
  implicit val arbByteBuffer: Arbitrary[ByteBuffer] = Arbitrary(byteBufferGen)
  implicit val arbPosInt: Arbitrary[PosInt] = Arbitrary(posIntGen)
  implicit val arbTestBufferState: Arbitrary[(List[String], TestBufferState)] =
    Arbitrary(
      Gen.oneOf(testBufferEmptyGen,
                testBuffer1MessageStateGen/*,
                testBuffer2MessagesStateGen*/))
}
