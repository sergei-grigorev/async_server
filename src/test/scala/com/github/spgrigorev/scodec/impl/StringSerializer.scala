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
        val maxPosition = buffer.limit()

        @tailrec
        def findNext(position: Int): Option[Int] = {
          if (position < maxPosition) {
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
            case Some(end) =>
              val squezedBuffer =
                buffer
                  .duplicate()
                  .position(start)
                  .limit(end)

              val command = charset
                .decode(squezedBuffer.asInstanceOf[ByteBuffer])
                .toString
              findAll(end + 1, (command, start) :: acc)
            case None =>
              acc.reverse
          }
        }

        val messages = findAll(buffer.position(), List.empty).toNel

        messages.fold(
          DeserializeError
            .notEnough()
            .asLeft[(ByteBuffer, NonEmptyList[String])]) { list =>
          val next = list.map(_._2).last
          val nextBuffer =
            buffer.position(next).asInstanceOf[ByteBuffer].compact()
          (nextBuffer, list.map(_._1)).asRight[DeserializeError]
        }
      }

      override def serialize(message: String): ByteBuffer =
        charset.encode(message)
    }
}
