package com.github.spgrigorev.scodec.domain

sealed trait ClientError extends Product with Serializable

object ClientError {
  case object ClosedConnection extends ClientError
  case object IncorrectProtocol extends ClientError
}
