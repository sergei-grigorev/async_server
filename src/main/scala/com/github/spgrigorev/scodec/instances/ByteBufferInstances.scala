package com.github.spgrigorev.scodec.instances

import java.nio.ByteBuffer

import com.github.spgrigorev.scodec.data.Buffer
import eu.timepit.refined.types.numeric.NonNegInt

trait ByteBufferInstances {
  implicit val byteBufferInstance: Buffer[ByteBuffer] =
    new Buffer[ByteBuffer] {
      override def asReadOnly(buffer: ByteBuffer): ByteBuffer = {
        buffer
          .duplicate()
          .asReadOnlyBuffer()
          .flip()
          .asInstanceOf[ByteBuffer]
      }

      override def isFull(buffer: ByteBuffer): Boolean = {
        buffer.position() == buffer.capacity()
      }

      override def increaseCapacity(buffer: ByteBuffer): ByteBuffer = {
        ByteBuffer
          .allocate(4 max (buffer.capacity() << 1))
          .put(buffer.flip().asInstanceOf[ByteBuffer])
      }

      override def compact(buffer: ByteBuffer, from: NonNegInt): ByteBuffer = {
        allocate(NonNegInt.unsafeFrom(buffer.capacity()))
          .put(
            buffer
              .duplicate()
              .position(from.value min buffer.limit())
              .asInstanceOf[ByteBuffer])
          .position(0)
          .asInstanceOf[ByteBuffer]
      }

      override def consumed(buffer: ByteBuffer): NonNegInt = {
        NonNegInt.unsafeFrom(buffer.position())
      }

      override def allocate(size: NonNegInt): ByteBuffer = {
        ByteBuffer.allocate(size.value)
      }
    }
}
