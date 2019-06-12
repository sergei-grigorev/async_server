package com.github.spgrigorev.scodec.impl

import com.github.spgrigorev.scodec.algebras.NetworkProtocol
import com.github.spgrigorev.scodec.basic.RequestResponseProtocol
import com.github.spgrigorev.scodec.codecs.BigNumeric.Int128
import com.github.spgrigorev.scodec.domain.ClientError
import com.github.spgrigorev.scodec.impl.MtProtoAuthProtocol.ConnectionState
import com.github.spgrigorev.scodec.model.MtProto.ResPQ.Fingerprints
import com.github.spgrigorev.scodec.model.{MtProto, MtProtoMessage}
import scalaz.zio.random.Random
import scalaz.zio.{IO, UIO, ZIO}
import cats.syntax.option._

trait MtProtoAuthProtocol[CONNECTION]
    extends NetworkProtocol[CONNECTION, MtProtoMessage] {

  // for unit tests to make it possible to fix some arbitrary values
  def serverNonceGen: IO[Nothing, Int128] =
    ZIO
      .accessM[Random] { env =>
        for {
          first <- env.random.nextLong
          second <- env.random.nextLong
        } yield Int128(first, second)
      }
      .provide(Random.Live)

  def pqGen: IO[Nothing, String] =
    ZIO
      .accessM[Random](_.random.nextLong.map(_.toHexString.take(8)))
      .provide(Random.Live)

  def fingerPrintsGen: IO[Nothing, Fingerprints] =
    ZIO
      .accessM[Random](_.random.nextLong)
      .map(key => Fingerprints(List(key)))
      .provide(Random.Live)

  def encryptedAnswerGen(p: String,
                         q: String,
                         publicKeyFingerPrint: Long,
                         encryptedData: String): IO[Nothing, String] =
    ZIO
      .accessM[Random] { env =>
        for {
          randomBytes <- env.random.nextBytes(596)
          encryptedAnswer <- UIO(randomBytes.map(_ % 16).mkString)
        } yield encryptedAnswer
      }
      .provide(Random.Live)

  override def protocol: NetworkProtocol.Service[CONNECTION, MtProtoMessage] =
    new RequestResponseProtocol[MtProtoMessage, ConnectionState, CONNECTION] {

      override def makeNewState(connection: CONNECTION): UIO[ConnectionState] =
        UIO.succeed(MtProtoAuthProtocol.ConnectionState(None, None))

      override def callback(message: MtProtoMessage, state: ConnectionState)
        : IO[ClientError, (List[MtProtoMessage], ConnectionState)] =
        message.message match {
          case MtProto.ReqPQ(nonce) =>
            val reqPq =
              for {
                serverNonce <- serverNonceGen
                pq <- pqGen
                fingerPrints <- fingerPrintsGen
              } yield MtProto.ResPQ(nonce, serverNonce, pq, fingerPrints)

            reqPq
              .map(
                reqPq =>
                  (List(MtProtoMessage(message.messageId + 1L, reqPq)),
                   state.copy(nonce = reqPq.nonce.some,
                              serverNonce = reqPq.serverNonce.some)))

          case MtProto.ReqDHParams(nonce,
                                   serverNonce,
                                   p,
                                   q,
                                   publicKeyFingerprint,
                                   encryptedData)
              if state.nonce.contains(nonce) && state.serverNonce.contains(
                serverNonce) =>
            val serverDHParamsOk =
              for {
                encryptedAnswer <- encryptedAnswerGen(p,
                                                      q,
                                                      publicKeyFingerprint,
                                                      encryptedData)
              } yield
                MtProto.ServerDHParamsOk(nonce, serverNonce, encryptedAnswer)

            serverDHParamsOk
              .map(ok =>
                (List(MtProtoMessage(message.messageId + 1L, ok)), state))

          case incorrectMessage =>
            ZIO.fail(
              ClientError.incorrectProtocol(
                s"unexpected message $incorrectMessage for a state $state"))
        }
    }
}

object MtProtoAuthProtocol {
  case class ConnectionState(nonce: Option[Int128], serverNonce: Option[Int128])
}
