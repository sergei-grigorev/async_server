package com.github.spgrigorev.scodec.basic

import cats.MonoidK
import cats.instances.list._
import cats.instances.unit._
import cats.kernel.Monoid
import cats.syntax.option._
import cats.syntax.semigroup._
import cats.syntax.traverse._
import com.github.spgrigorev.scodec.algebras.NetworkProtocol.ServiceEnvironment
import com.github.spgrigorev.scodec.algebras.Serializer.SerializeError
import com.github.spgrigorev.scodec.algebras.Serializer.algebra._
import com.github.spgrigorev.scodec.algebras.{Connection, Logger, NetworkProtocol, Serializer}
import com.github.spgrigorev.scodec.basic.RequestResponseProtocolSpecTemplate.{TestBufferState, TypedTest}
import com.github.spgrigorev.scodec.domain.ClientError
import eu.timepit.refined.types.numeric.NonNegInt
import scalaz.zio._
import scalaz.zio.interop.catz._
import scodec.bits.BitVector

import scala.Option.empty

trait RequestResponseProtocolTemplate[MESSAGE] extends DefaultRuntime {

  val protocolToTest: NetworkProtocol.Service[Unit, MESSAGE]

  def environment(ref: Ref[TestBufferState]): ServiceEnvironment[Unit, MESSAGE]

  def runZioTest[A](data: TypedTest[A]): IO[ClientError, List[MESSAGE]] = {
    for {
      stateRef <- Ref.make(TestBufferState(data.readingArrays))
      _ <- protocolToTest.registerClient(()).provide(environment(stateRef))
      state <- stateRef.get
      reversed = state.outputReversed.reverse
      deserialized <- reversed
        .traverse[ZIO[Serializer[MESSAGE], ClientError, ?],
                  (BitVector, MESSAGE)] {
          deserialize[MESSAGE](_).mapError {
            case SerializeError.NotEnough =>
              ClientError.incorrectProtocol(
                "server generated incomplete message")
            case SerializeError.WrongContent(error) =>
              ClientError.incorrectProtocol(error)
          }
        }
        .provide(environment(stateRef))
    } yield deserialized.map(_._2)
  }
}

object RequestResponseProtocolSpecTemplate {
  final case class TypedTest[A](readingArrays: List[BitVector])

  object TypedTest {
    implicit val typedTestMonoid: MonoidK[TypedTest] = new MonoidK[TypedTest] {
      override def empty[A]: TypedTest[A] = TypedTest[A](List.empty[BitVector])

      override def combineK[A](x: TypedTest[A], y: TypedTest[A]): TypedTest[A] =
        TypedTest[A](x.readingArrays |+| y.readingArrays)
    }
  }

  /**
    * Intermediate state in tests.
    */
  final case class TestBufferState(
      readingArrays: List[BitVector],
      outputReversed: List[BitVector]
  ) {
    def addOutput(value: BitVector): TestBufferState =
      this.copy(outputReversed = value :: outputReversed)

    def getInput: Option[(BitVector, TestBufferState)] =
      readingArrays match {
        case Nil => empty
        case head :: tail =>
          (head, this.copy(readingArrays = tail)).some
      }

    def returnInput(value: BitVector): TestBufferState =
      this.copy(readingArrays = value :: readingArrays)

  }

  object TestBufferState {
    def apply(readingArrays: List[BitVector]): TestBufferState =
      TestBufferState(readingArrays, outputReversed = List.empty)

    implicit val testBufferStateMonoid: Monoid[TestBufferState] =
      new Monoid[TestBufferState] {
        override def empty: TestBufferState = TestBufferState(List.empty)

        override def combine(a: TestBufferState,
                             b: TestBufferState): TestBufferState =
          TestBufferState(a.readingArrays |+| b.readingArrays,
                          a.outputReversed |+| b.outputReversed)
      }
  }

  /**
    * Implementation of environment supposed to use in tests.
    */
  abstract class TestEnvironment(stateRef: Ref[TestBufferState])
      extends Connection[Unit]
      with Logger {

    val readBytesBucketSize: NonNegInt

    override val logger: Logger.Service = new Logger.Service {
      override def info(message: String): ZIO[Logger, Nothing, Unit] = ZIO.unit
      override def error(error: String): ZIO[Logger, Nothing, Unit] = ZIO.unit
      override def warn(message: String): ZIO[Logger, Nothing, Unit] = ZIO.unit
    }

    override val connection: Connection.Service[Unit] =
      new Connection.Service[Unit] {
        override def read(buffer: BitVector,
                          connection: Unit): IO[ClientError, BitVector] = {
          for {
            state <- stateRef.get
            buffer <- state.getInput.fold[IO[ClientError, BitVector]](
              ZIO.fail(ClientError.closedConnection)) {
              case (array, newState)
                  if array.toByteVector.length <= readBytesBucketSize.value =>
                stateRef.set(newState) *> UIO(buffer ++ array)
              case (array, newState) =>
                val (subArray, remainder) =
                  array.toByteVector.splitAt(readBytesBucketSize.value)
                stateRef.set(newState.returnInput(remainder.toBitVector)) *>
                  UIO(buffer ++ subArray.toBitVector)
            }
          } yield buffer
        }

        override def write(buffer: BitVector,
                           connection: Unit): IO[ClientError, Unit] =
          stateRef.update(_.addOutput(buffer)).unit

        override def disconnect(connection: Unit): UIO[Unit] = UIO.unit
      }
  }
}
