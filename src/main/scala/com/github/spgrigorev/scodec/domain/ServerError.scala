package com.github.spgrigorev.scodec.domain

import cats.data.NonEmptyList

sealed trait ServerError extends Product with Serializable

object ServerError {
  case object PortIsNotAvailable extends ServerError
  final case class ConfigurationError(errors: NonEmptyList[String])
      extends ServerError
  final case class InterfaceError(cause: Throwable) extends ServerError
  final case object ServerStopped extends ServerError
}
