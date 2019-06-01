package com.github.spgrigorev.scodec.instances

import cats.Show

trait ThrowableInstances {
  implicit val errorShow: Show[Throwable] = { e =>
    s"Error message: ${e.getMessage}"
  }
}
