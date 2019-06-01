package com.github.spgrigorev.scodec.data

import eu.timepit.refined.types.numeric.NonNegInt

/**
  * Type class to work with intermediate IO buffers.
  *
  * @tparam B buffer type
  */
trait Buffer[B] {

  /**
    * Return some mock that are not allowed to be changed.
    */
  def asReadOnly(b: B): B

  /**
    * Checks if buffer is full and required to be extended.
    */
  def isFull(b: B): Boolean

  /**
    * Increase capacity for buffer.
    */
  def increaseCapacity(b: B): B

  /**
    * Shift content and potentially can squeeze buffer size.
    */
  def compact(b: B, from: NonNegInt): B

  /**
    * Calculate shifting of already consumed values.
    */
  def consumed(b: B): NonNegInt

  /**
    * Allocate new empty buffer.
    */
  def allocate(size: NonNegInt): B
}

object Buffer {
  def apply[B](implicit instance: Buffer[B]): Buffer[B] = instance
}
