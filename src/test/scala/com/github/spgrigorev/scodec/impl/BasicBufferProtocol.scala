package com.github.spgrigorev.scodec.impl

import java.nio.ByteBuffer

import cats.data.NonEmptyList
import com.github.spgrigorev.scodec.algebras.NetworkProtocol
import com.github.spgrigorev.scodec.algebras.NetworkProtocol.ServiceEnvironment
import com.github.spgrigorev.scodec.algebras.Serializer.DeserializeError
import com.github.spgrigorev.scodec.domain.ClientError
import com.github.spgrigorev.scodec.impl.BasicBufferProtocol.ConnectionState
import scalaz.zio.{Ref, ZIO}

trait BasicBufferProtocol[B, C] extends NetworkProtocol[C, ByteBuffer, B] {

  import com.github.spgrigorev.scodec.algebras.Connection.algebra._
  import com.github.spgrigorev.scodec.algebras.Serializer.algebra._

  override val protocol: NetworkProtocol.Service[C, ByteBuffer, B] =
    (connection: C) => {
      def listener(buffer: ByteBuffer, state: Ref[ConnectionState[B]])
        : ZIO[ServiceEnvironment[C, ByteBuffer, B], ClientError, Unit] = {
        read(buffer, connection)
          .flatMap { buffer =>
            deserialize[ByteBuffer, B](buffer)
              .flatMap {
                case Left(DeserializeError.NotEnough) =>
                  if (buffer.limit() < buffer.position())
                    listener(buffer, state)
                  else
                    listener(ByteBuffer
                               .allocate(buffer.limit() >> 2)
                               .put(buffer),
                             state)

                case Left(DeserializeError.WrongContent(_)) =>
                  ZIO.fail(ClientError.ClosedConnection)

                case Right((updBuffer, NonEmptyList(head, _))) =>
                  state
                    .update(_.copy(prevMessage = Some(head)))
                    .map(_.prevMessage)
                    .flatMap {
                      // send previous if it is available
                      // then loop
                      _.fold(ZIO.unit *> listener(updBuffer, state)) {
                        prevState =>
                          serialize[B, ByteBuffer](prevState)
                            .flatMap(write(_, connection))
                            .flatMap(_ => listener(updBuffer, state))
                      }
                    }
              }

          }
          .catchSome {
            case ClientError.ClosedConnection =>
              ZIO.unit
          }
      }

      for {
        state <- Ref.make(ConnectionState(Option.empty[B]))
        _ <- listener(ByteBuffer.allocate(1024), state)
      } yield ()
    }
}

object BasicBufferProtocol {
  case class ConnectionState[B](prevMessage: Option[B])
}
