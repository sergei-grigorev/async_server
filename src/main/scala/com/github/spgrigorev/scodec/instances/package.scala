package com.github.spgrigorev.scodec

package object instances {
  object all
      extends ThrowableInstances
      with AsynchronousSocketChannelInstances
      with ByteBufferInstances

  object throwable extends ThrowableInstances
  object clientChannel extends AsynchronousSocketChannelInstances
  object byteBuffer extends ByteBufferInstances
}
