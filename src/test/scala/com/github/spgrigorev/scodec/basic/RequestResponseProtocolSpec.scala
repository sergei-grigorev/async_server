package com.github.spgrigorev.scodec.basic

import java.nio.ByteBuffer

import cats.instances.list._
import cats.instances.string._
import cats.kernel.laws._
import cats.kernel.laws.discipline._
import cats.syntax.option._
import com.github.spgrigorev.scodec.ArbitraryInstances
import com.github.spgrigorev.scodec.algebras.NetworkProtocol.ServiceEnvironment
import com.github.spgrigorev.scodec.algebras.{Connection, Logger}
import com.github.spgrigorev.scodec.basic.RequestResponseProtocolSpec.TestBufferState
import com.github.spgrigorev.scodec.domain.ClientError
import com.github.spgrigorev.scodec.impl.StringSerializer
import com.github.spgrigorev.scodec.instances.byteBuffer._
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.auto._
import org.scalacheck.Prop
import org.scalatest.FunSuiteLike
import org.scalatestplus.scalacheck.Checkers
import scalaz.zio._

import scala.Option.empty

class RequestResponseProtocolSpec
    extends DefaultRuntime
    with FunSuiteLike
    with Checkers
    with ArbitraryInstances {

  /**
    * our test object duplicates every input message.
    */
  private val testObject =
    new RequestResponseProtocol[ByteBuffer, String, Unit, Unit] {
      override def makeNewState(connection: Unit): UIO[Ref[Unit]] =
        Ref.make(())

      override def callback(messages: String,
                            state: Ref[Unit]): IO[ClientError, List[String]] =
        UIO(messages :: messages :: Nil)

      override val initBufferSize: NonNegInt = 4
    }

  private def runZioTest(data: TestBufferState): IO[ClientError, List[String]] =
    for {
      stateRef <- Ref.make(data)
      _ <- testObject
        .registerClient(())
        .provide(RequestResponseProtocolSpec.TestEnvironment(stateRef))
      state <- stateRef.get
    } yield
      state.outputReversed.reverse
        .map(bwz =>
          bwz.duplicate().limit(bwz.limit() - 1).asInstanceOf[ByteBuffer])
        .map(charset.decode)
        .map(_.toString)

  test(
    "tested request-response protocol always returns duplicated input message") {
    check(
      Prop.forAllNoShrink { a: (List[String], TestBufferState) =>
        a match {
          case (orig, state) =>
            val result = unsafeRun(runZioTest(state))
            val expected = orig.flatMap(el => el :: el :: Nil)
            result <-> expected
        }
      }
    )
  }
}

object RequestResponseProtocolSpec {
  type TestServiceEnv = ServiceEnvironment[Unit, ByteBuffer, String]

  /**
    * Intermediate state in tests.
    */
  final case class TestBufferState(
      readingArrays: List[ByteBuffer],
      outputReversed: List[ByteBuffer]
  ) {
    def addOutput(value: ByteBuffer): TestBufferState =
      this.copy(outputReversed = value :: outputReversed)

    def getInput: Option[(ByteBuffer, TestBufferState)] =
      readingArrays match {
        case Nil => empty
        case head :: tail =>
          (head, this.copy(readingArrays = tail)).some
      }

    def returnInput(value: ByteBuffer): TestBufferState =
      this.copy(readingArrays = value :: readingArrays)

  }

  object TestBufferState {
    def apply(readingArrays: List[ByteBuffer]): TestBufferState =
      TestBufferState(readingArrays, outputReversed = List.empty)
  }

  /**
    * Implementation of environment supposed to use in tests.
    */
  final case class TestEnvironment(stateRef: Ref[TestBufferState])
      extends Connection[Unit, ByteBuffer]
      with StringSerializer
      with Logger {
    override val logger: Logger.Service = new Logger.Service {
      override def info(message: String): ZIO[Logger, Nothing, Unit] = ZIO.unit
      override def error(error: String): ZIO[Logger, Nothing, Unit] = ZIO.unit
    }

    override val connection: Connection.Service[Unit, ByteBuffer] =
      new Connection.Service[Unit, ByteBuffer] {
        override def read(buffer: ByteBuffer,
                          connection: Unit): IO[ClientError, ByteBuffer] = {
          val copy = ByteBuffer.allocate(buffer.capacity())
          copy.put(buffer.duplicate().flip().asInstanceOf[ByteBuffer])
          val availablePlace = copy.capacity() - copy.position()

          for {
            state <- stateRef.get
            buffer <- state.getInput.fold[IO[ClientError, ByteBuffer]](
              ZIO.fail(ClientError.ClosedConnection)) {
              case (array, newState) if availablePlace >= array.remaining() =>
                stateRef.set(newState) *> UIO(copy.put(array))
              case (array, newState) =>
                val subArray =
                  array
                    .duplicate()
                    .flip()
                    .limit(availablePlace)
                    .asInstanceOf[ByteBuffer]
                val remained =
                  byteBufferInstance
                    .compact(array, NonNegInt.unsafeFrom(availablePlace))
                    .limit(array.limit() - availablePlace)
                    .asInstanceOf[ByteBuffer]
                stateRef.set(newState.returnInput(remained)) *> UIO(
                  copy.put(subArray))
            }
          } yield buffer
        }

        override def write(buffer: ByteBuffer,
                           connection: Unit): IO[ClientError, Unit] =
          stateRef.update(_.addOutput(buffer)).unit

        override def disconnect(connection: Unit): UIO[Unit] = UIO.unit
      }
  }
}
