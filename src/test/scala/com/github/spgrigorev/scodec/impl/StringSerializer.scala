package com.github.spgrigorev.scodec.impl

import java.nio.ByteBuffer
import java.nio.charset.Charset

import cats.data.NonEmptyList
import cats.syntax.either._
import cats.syntax.list._
import cats.syntax.option._
import com.github.spgrigorev.scodec.algebras.Serializer
import com.github.spgrigorev.scodec.algebras.Serializer.DeserializeError

import scala.annotation.tailrec

trait StringSerializer extends Serializer[ByteBuffer, String] {
  override val serializer: Serializer.Service[ByteBuffer, String] =
    new Serializer.Service[ByteBuffer, String] {

      private val charset: Charset = Charset.forName("UTF-8")

      /**
        * Find all available messages in a buffer.
        */
      override def deserialize(
          buffer: ByteBuffer): Either[Serializer.DeserializeError,
                                      (ByteBuffer, NonEmptyList[String])] = {
        val rightBound = buffer.limit()

        @tailrec
        def findNext(position: Int): Option[Int] = {
          if (position < rightBound) {
            if (buffer.get(position) == '\0')
              position.some
            else findNext(position + 1)
          } else {
            none
          }
        }

        @tailrec
        def findAll(start: Int,
                    acc: List[(String, Int)]): List[(String, Int)] = {
          findNext(start) match {
            case Some(end) if end == 0 || end > start =>
              val squeezedBuffer =
                buffer
                  .duplicate()
                  .position(start)
                  .limit(end)
                  .asInstanceOf[ByteBuffer]

              val command = charset.decode(squeezedBuffer).toString
              findAll(end + 1, (command, end) :: acc)
            case _ =>
              acc.reverse
          }
        }

        val messages = findAll(0, List.empty).toNel

        messages.fold(
          DeserializeError
            .notEnough()
            .asLeft[(ByteBuffer, NonEmptyList[String])]) { list =>
          val nonConsumedNext = list.map(_._2).last + 1

          val nextBuffer =
            if (nonConsumedNext == rightBound) {
              buffer
                .duplicate()
                .position(0)
                .limit(0)
                .asInstanceOf[ByteBuffer]
            } else {
              buffer
                .duplicate()
                .position(nonConsumedNext)
                .asInstanceOf[ByteBuffer]
            }

          (nextBuffer, list.map(_._1)).asRight[DeserializeError]
        }
      }

      override def serialize(message: String): ByteBuffer =
        charset.encode(message + '\0')
    }
}
