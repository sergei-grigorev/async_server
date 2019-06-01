package com.github.spgrigorev.scodec.domain

sealed trait OutputContent extends Product with Serializable

object OutputContent {
  case class Chunk[A](content: A) extends OutputContent
  case object Disconnect extends OutputContent
}
