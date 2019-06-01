package com.github.spgrigorev.scodec.laws

import com.github.spgrigorev.scodec.data.Buffer
import cats.kernel.laws._
import eu.timepit.refined.types.numeric.{NonNegInt, PosInt}
import eu.timepit.refined.auto._

trait BufferLaws[B] {
  def algebra: Buffer[B]

  def newNonEmptyAllocatedIsNotFull(size: PosInt): IsEq[Boolean] =
    algebra.isFull(algebra.allocate(size)) <-> false

  def newAllocatedStartPositionIsZero(size: NonNegInt): IsEq[NonNegInt] =
    algebra.consumed(algebra.allocate(size)) <-> 0

  def zeroSizeBufferIsFullAlways(): IsEq[Boolean] =
    algebra.isFull(algebra.allocate(0)) <-> true

  def compactionShiftsStartPosition(buffer: B,
                                    from: NonNegInt): IsEq[NonNegInt] =
    algebra.consumed(algebra.compact(buffer, from)) <-> 0

  def increaseCapacityDoesntShiftStartPosition(buffer: B): IsEq[NonNegInt] = {
    algebra.consumed(buffer) <-> algebra.consumed(
      algebra.increaseCapacity(buffer))
  }

  def increaseCapacityAlwaysClearsIsFullFlag(buffer: B): IsEq[Boolean] =
    algebra.isFull(algebra.increaseCapacity(buffer)) <-> false
}

object BufferLaws {
  def apply[B](instance: Buffer[B]): BufferLaws[B] = new BufferLaws[B] {
    override def algebra: Buffer[B] = instance
  }
}
