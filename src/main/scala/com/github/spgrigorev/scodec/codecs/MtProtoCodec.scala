package com.github.spgrigorev.scodec.codecs

import com.github.spgrigorev.scodec.codecs.BigNumeric._
import com.github.spgrigorev.scodec.model.{MtProto, MtProtoMessage}
import com.github.spgrigorev.scodec.model.MtProto.ResPQ.Fingerprints
import com.github.spgrigorev.scodec.model.MtProto._
import scodec.Codec
import scodec.codecs._
import shapeless.HNil
import shapeless.syntax.singleton._

object MtProtoCodec {

  implicit val mtProtoCodec: Codec[MtProto] = {
    val fingerprintsCodec: Codec[Fingerprints] = (
      ("%(Vector long)" | constant(0x1cb5c415)) ::
        ("fingerpints" | listOfN(int32, int64))
    ).as[Fingerprints]

    implicit def reqPQCodec: Codec[ReqPQ] =
      ("nonce" | int128).as[ReqPQ]

    implicit def resPqCodec: Codec[ResPQ] =
      (
        ("nonce" | int128) ::
          ("server_nonce" | int128) ::
          ("pq" | natNumber) ::
          ("server_public_key_fingerprints:Vector" | fingerprintsCodec)
      ).as[ResPQ]

    implicit def reqDHCodec: Codec[ReqDHParams] =
      (
        ("nonce" | int128) ::
          ("server_nonce" | int128) ::
          ("p" | natNumber) ::
          ("q" | natNumber) ::
          ("public_key_fingerprint" | int64) ::
          ("encrypted_data" | fixedSizeBytes(260, ascii))
      ).as[ReqDHParams]

    implicit def serverDHParamsCodec: Codec[ServerDHParamsOk] =
      (
        ("nonce" | int128) ::
          ("server_nonce" | int128) ::
          ("encrypted_data" | fixedSizeBytes(596, ascii))
      ).as[ServerDHParamsOk]

    Codec
      .coproduct[MtProto]
      .discriminatedBy(int32)
      .using(
        'ReqPQ ->> 0x60469778 ::
          'ResPQ ->> 0x05162463 ::
          'ReqDHParams ->> 0xd712e4be ::
          'ServerDHParamsOk ->> 0xd0e8075c ::
          HNil
      )
      .as[MtProto]
  }

  private val unencrypted = constant(0x00000000)

  implicit val untypedMessageCodec: Codec[MtProtoMessage] = (
    ("auth_key_id" | unencrypted) ::
      ("message_id" | int64) ::
      ("message" | variableSizeBytes(int32, Codec[MtProto]))
  ).as[MtProtoMessage]

}
