package com.github.spgrigorev.scodec.basic

import cats.instances.list._
import cats.syntax.traverse._
import com.github.spgrigorev.scodec.algebras.NetworkProtocol
import com.github.spgrigorev.scodec.algebras.NetworkProtocol.ServiceEnvironment
import com.github.spgrigorev.scodec.algebras.Serializer.DeserializeError
import com.github.spgrigorev.scodec.data.Buffer
import com.github.spgrigorev.scodec.domain.ClientError
import com.github.spgrigorev.scodec.syntax.buffer._
import eu.timepit.refined.types.numeric.NonNegInt
import scalaz.zio.interop.catz._
import scalaz.zio.{IO, Ref, UIO, ZIO}

/**
  * Basic protocol with [request -> response] model.
  *
  * @tparam BUFFER     IO Buffer type
  * @tparam MESSAGE    Deserialized messages type
  * @tparam STATE      intermediate state
  * @tparam CONNECTION network connection type
  */
trait RequestResponseProtocol[BUFFER, MESSAGE, STATE, CONNECTION]
    extends NetworkProtocol.Service[CONNECTION, BUFFER, MESSAGE] {

  type ClientErrorIO[A] =
    ZIO[ServiceEnvironment[CONNECTION, BUFFER, MESSAGE], ClientError, A]

  import com.github.spgrigorev.scodec.algebras.Connection.algebra._
  import com.github.spgrigorev.scodec.algebras.Serializer.algebra._

  def makeNewState(connection: CONNECTION): UIO[Ref[STATE]]

  def callback(messages: MESSAGE,
               state: Ref[STATE]): IO[ClientError, List[MESSAGE]]

  val initBufferSize: NonNegInt

  final override def registerClient(connection: CONNECTION)(
      implicit BUFFER: Buffer[BUFFER]): ClientErrorIO[Unit] = {
    def listener(buffer: BUFFER, state: Ref[STATE]): ClientErrorIO[Unit] = {
      read(buffer, connection)
        .flatMap { buffer =>
          deserialize[BUFFER, MESSAGE](buffer.asReadOnly)
            .flatMap {
              case Left(DeserializeError.NotEnough) =>
                if (!BUFFER.isFull(buffer)) {
                  listener(buffer, state)
                } else {
                  listener(buffer.increaseCapacity, state)
                }

              case Left(DeserializeError.WrongContent(_)) =>
                ZIO.fail(ClientError.ClosedConnection)

              case Right((remained, messages)) =>
                val response =
                  messages.toList
                    .traverse[ClientErrorIO, List[MESSAGE]](m =>
                      callback(m, state))
                    .map(_.flatten)

                val output =
                  response
                    .flatMap { outputList =>
                      outputList.traverse[ClientErrorIO, Unit] { m =>
                        serialize(m) >>= write(connection)
                      }
                    }

                output *>
                  listener(buffer.compact(remained.consumed), state)
            }

        }
        .catchSome {
          case ClientError.ClosedConnection =>
            ZIO.unit
        }
    }

    for {
      state <- this.makeNewState(connection)
      _ <- listener(BUFFER.allocate(initBufferSize), state)
    } yield ()
  }
}
