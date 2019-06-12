package com.github.spgrigorev.scodec

package object instances {
  object all
      extends ThrowableInstances
      with AsynchronousSocketChannelInstances

  object throwable extends ThrowableInstances
  object clientChannel extends AsynchronousSocketChannelInstances
}
