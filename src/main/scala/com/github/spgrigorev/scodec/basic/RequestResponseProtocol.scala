package com.github.spgrigorev.scodec.basic

import cats.Show
import cats.instances.list._
import cats.syntax.traverse._
import com.github.spgrigorev.scodec.algebras.Connection.algebra._
import com.github.spgrigorev.scodec.algebras.Logger.algebra._
import com.github.spgrigorev.scodec.algebras.NetworkProtocol
import com.github.spgrigorev.scodec.algebras.NetworkProtocol.ServiceEnvironment
import com.github.spgrigorev.scodec.algebras.Serializer.SerializeError
import com.github.spgrigorev.scodec.algebras.Serializer.algebra._
import com.github.spgrigorev.scodec.domain.ClientError
import scalaz.zio.interop.catz._
import scalaz.zio.{IO, UIO, ZIO}
import scodec.bits.BitVector

/**
  * Basic protocol with [request -> response] model.
  *
  * @tparam MESSAGE    Deserialized messages type
  * @tparam STATE      intermediate state
  * @tparam CONNECTION network connection type
  */
trait RequestResponseProtocol[MESSAGE, STATE, CONNECTION]
    extends NetworkProtocol.Service[CONNECTION, MESSAGE] {

  type FixedEnvironment =
    ServiceEnvironment[CONNECTION, MESSAGE]

  private type ClientErrorIO[A] =
    ZIO[FixedEnvironment, ClientError, A]

  def makeNewState(connection: CONNECTION): UIO[STATE]

  def callback(message: MESSAGE,
               state: STATE): IO[ClientError, (List[MESSAGE], STATE)]

  final override def registerClient(connection: CONNECTION)(
      implicit connectionShow: Show[CONNECTION]): ClientErrorIO[Unit] = {

    def next(buffer: BitVector)(
        state: STATE): ZIO[FixedEnvironment, Nothing, Unit] =
      listener(buffer, state)

    def listener(buffer: BitVector,
                 state: STATE): ZIO[FixedEnvironment, Nothing, Unit] = {

      deserialize[MESSAGE](buffer)
        .foldM[FixedEnvironment, ClientError, Unit](
          {
            case SerializeError.NotEnough =>
              read(buffer, connection).flatMap(buffer =>
                listener(buffer, state))

            case SerializeError.WrongContent(message) =>
              error(message, connection) *>
                ZIO.fail(ClientError.closedConnection)
          }, {
            case (remained, message) =>
              val updState =
                for {
                  response <- callback(message, state)
                  (outputMessages, updState) = response

                  serialized <- outputMessages
                    .traverse[ClientErrorIO, BitVector] { m =>
                      serialize(m).mapError {
                        case SerializeError.NotEnough =>
                          ClientError.incorrectProtocol(
                            "server generated incomplete message")
                        case SerializeError.WrongContent(error) =>
                          ClientError.incorrectProtocol(error)
                      }
                    }
                  _ <- serialized.traverse[ClientErrorIO, Unit](
                    write(connection))
                } yield updState

              updState >>= next(remained)
          }
        )
        .catchAll {
          case ClientError.ClosedConnection =>
            ZIO.unit

          case ClientError.ReadTimeout =>
            warn("read timeout", connection) *> ZIO.unit

          case ClientError.WriteTimeout =>
            warn("write timeout", connection) *> ZIO.unit

          case ClientError.IncorrectProtocol(error) =>
            warn("incorrect protocol: " + error, connection) *> ZIO.unit
        }
    }

    for {
      state <- this.makeNewState(connection)
      _ <- listener(BitVector.empty, state)
    } yield ()
  }
}
