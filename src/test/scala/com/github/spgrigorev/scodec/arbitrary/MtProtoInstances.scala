package com.github.spgrigorev.scodec.arbitrary

import com.github.spgrigorev.scodec.algebras.Serializer
import com.github.spgrigorev.scodec.basic.RequestResponseProtocolSpecTemplate.TypedTest
import com.github.spgrigorev.scodec.codecs.BigNumeric.Int128
import com.github.spgrigorev.scodec.impl.MtProtoSerializer
import com.github.spgrigorev.scodec.model.{MtProto, MtProtoMessage}
import org.scalacheck.{Arbitrary, Gen}
import scodec.bits.BitVector

trait MtProtoInstances {
  private val mtProtoSerializer: Serializer.Service[MtProtoMessage] =
    new MtProtoSerializer {}.serializer

  private val longGen: Gen[Long] = Gen.chooseNum(Long.MinValue, Long.MaxValue)
  private val number8bitsTextGen: Gen[String] =
    longGen.map(_.toHexString.take(7))

  val int128Gen: Gen[Int128] = for {
    firstPart <- longGen
    secondPart <- longGen
  } yield Int128(firstPart, secondPart)

  val hexChar: Gen[Char] = Gen.oneOf(Gen.numChar, Gen.choose('a', 'f'))

  def reqPqGen(nonce: Int128): Gen[MtProto.ReqPQ] =
    Gen.const(MtProto.ReqPQ(nonce))

  def resPqGen(nonce: Int128, serverNonce: Int128): Gen[MtProto.ResPQ] =
    for {
      pq <- number8bitsTextGen
      serverPublicKey <- Gen
        .nonEmptyListOf(longGen)
        .map(MtProto.ResPQ.Fingerprints)
    } yield MtProto.ResPQ(nonce, serverNonce, pq, serverPublicKey)

  def reqDHParamsGen(nonce: Int128,
                     serverNonce: Int128): Gen[MtProto.ReqDHParams] =
    for {
      p <- number8bitsTextGen
      q <- number8bitsTextGen
      publicKeyFingerprint <- longGen
      encryptedData <- Gen
        .listOfN(260, hexChar)
        .map(_.mkString)
    } yield
      MtProto.ReqDHParams(nonce,
                          serverNonce,
                          p,
                          q,
                          publicKeyFingerprint,
                          encryptedData)

  def serverDhParamsOkGen(nonce: Int128,
                          serverNonce: Int128): Gen[MtProto.ServerDHParamsOk] =
    for {
      encryptedAnswer <- Gen
        .listOfN(596, hexChar)
        .map(_.mkString)
    } yield MtProto.ServerDHParamsOk(nonce, serverNonce, encryptedAnswer)

  val untypedMessageGen: Gen[MtProtoMessage] = for {
    messageId <- longGen.filter(_ >= 0)
    nonce <- int128Gen
    serverNonce <- int128Gen
    message <- Gen.oneOf(reqPqGen(nonce),
                         resPqGen(nonce, serverNonce),
                         reqDHParamsGen(nonce, serverNonce),
                         serverDhParamsOkGen(nonce, serverNonce))
  } yield MtProtoMessage(messageId, message)

  val mtProtoMessageBitVectorsGen: Gen[(Int128, TypedTest[MtProtoMessage])] =
    for {
      nonce <- int128Gen
      serverNonce = Int128(0, 0)
      reqPq <- reqPqGen(nonce)
      reqPqSerialized = mtProtoSerializer
        .serialize(MtProtoMessage(0L, reqPq))
        .getOrElse(BitVector.empty)
      reqDHParams <- reqDHParamsGen(nonce, serverNonce)
      reqDHParamsSerialized = mtProtoSerializer
        .serialize(MtProtoMessage(2L, reqDHParams))
        .getOrElse(BitVector.empty)
    } yield
      (nonce,
       TypedTest[MtProtoMessage](List(reqPqSerialized, reqDHParamsSerialized)))

  implicit val arbUntypedMessage: Arbitrary[MtProtoMessage] =
    Arbitrary(untypedMessageGen)

  implicit val arbMtProtoState: Arbitrary[(Int128, TypedTest[MtProtoMessage])] =
    Arbitrary(mtProtoMessageBitVectorsGen)
}
