package com.github.spgrigorev.scodec.model

import cats.Show
import eu.timepit.refined.types.net.PortNumber

case class Config(port: PortNumber, interface: String)

object Config {
  implicit val configShow: Show[Config] = { config =>
    s"Config{interface: ${config.interface}, port: ${config.port}}"
  }
}
