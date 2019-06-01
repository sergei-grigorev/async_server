package com.github.spgrigorev.scodec.syntax

import com.github.spgrigorev.scodec.data.Buffer
import eu.timepit.refined.types.numeric.NonNegInt

import scala.language.implicitConversions

trait BufferSyntax {
  implicit def bufferSyntaxOps[B: Buffer](b: B): BufferOps[B] =
    new BufferOps[B](b)
}

final class BufferOps[B: Buffer](b: B) {
  def asReadOnly: B = Buffer[B].asReadOnly(b)

  def isFull: Boolean = Buffer[B].isFull(b)

  def increaseCapacity: B = Buffer[B].increaseCapacity(b)

  def compact(from: NonNegInt): B = Buffer[B].compact(b, from)

  def consumed: NonNegInt = Buffer[B].consumed(b)
}
