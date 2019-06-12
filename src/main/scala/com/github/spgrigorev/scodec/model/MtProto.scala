package com.github.spgrigorev.scodec.model

import com.github.spgrigorev.scodec.codecs.BigNumeric.Int128
import com.github.spgrigorev.scodec.model.MtProto.ResPQ.Fingerprints

sealed trait MtProto extends Product with Serializable

object MtProto {

  /**
    * req_pq#60469778
    * @param nonce int128 = ResPQ
    */
  final case class ReqPQ(nonce: Int128) extends MtProto


  /**
    * resPQ#05162463
    * @param nonce int128
    * @param serverNonce int128
    * @param pq string (8 bytes length)
    * @param serverPublicKey Vector long = ResPQ
    */
  final case class ResPQ(nonce: Int128,
                         serverNonce: Int128,
                         pq: String,
                         serverPublicKey: Fingerprints)
      extends MtProto

  object ResPQ {

    case class Fingerprints(fingerprints: List[Long])

  }

  /**
    * req_DH_params#d712e4be
    * @param nonce int128
    * @param serverNonce int128
    * @param p string (8 bytes length)
    * @param q string (8 bytes length)
    * @param publicKeyFingerprint long
    * @param encryptedData string (260 bytes length) = Server_DH_Params
    */
  final case class ReqDHParams(nonce: Int128,
                               serverNonce: Int128,
                               p: String,
                               q: String,
                               publicKeyFingerprint: Long,
                               encryptedData: String)
      extends MtProto

  /**
    * server_DH_params_ok#d0e8075c
    * @param nonce int128
    * @param serverNonce int128
    * @param encryptedAnswer string (596 bytes length) = Server_DH_Params
    */
  final case class ServerDHParamsOk(nonce: Int128,
                                    serverNonce: Int128,
                                    encryptedAnswer: String)
      extends MtProto

}
