package com.github.spgrigorev.scodec.domain

sealed trait ClientError extends Product with Serializable

object ClientError {
  case object ClosedConnection extends ClientError
  case object ReadTimeout extends ClientError
  case object WriteTimeout extends ClientError
  case class IncorrectProtocol(errorMessage: String) extends ClientError

  def incorrectProtocol(errorMessage: String): ClientError =
    IncorrectProtocol(errorMessage)

  def closedConnection: ClientError = ClosedConnection
}
