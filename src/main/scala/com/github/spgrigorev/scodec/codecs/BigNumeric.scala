package com.github.spgrigorev.scodec.codecs

import java.nio.charset.Charset

import scodec.Codec
import scodec.codecs._
import scodec.codecs.implicits._

object BigNumeric {
  val asciiCharset: Charset = Charset.forName("US-ASCII")

  case class Int128(first: Long, second: Long)

  val int128: Codec[Int128] = Codec[Int128]

  val ascii8: Codec[String] =
    variableSizeBytes(int8, string(asciiCharset))
      .withToString(s"string8(${asciiCharset.displayName})")

  val natNumber: Codec[String] =
    paddedFixedSizeBytes(8, ascii8, constant(0))
}
