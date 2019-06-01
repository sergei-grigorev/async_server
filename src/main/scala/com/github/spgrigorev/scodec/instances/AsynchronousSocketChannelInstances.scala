package com.github.spgrigorev.scodec.instances

import java.nio.channels.AsynchronousSocketChannel

import cats.Show

trait AsynchronousSocketChannelInstances {
  implicit val channelShow: Show[AsynchronousSocketChannel] =
    (t: AsynchronousSocketChannel) => s"${t.getRemoteAddress.toString}"
}
