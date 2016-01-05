package io.github.morgaroth.msc.quide.core.model

import io.github.morgaroth.msc.quide.core.model.Complex._

/**
  * Created by mateusz on 04.01.16.
  */
case class QbitValue(`a|0>`: `|0>`, `b|1>`: `|1>`) {
  def apply(o: Operator) = {
    if (o.size == 1) {
      QbitValue(o(0, 0) * `a|0>` + o(0, 1) * `b|1>`, o(1, 0) * `a|0>` + o(1, 1) * `b|1>`)
    } else throw new IllegalArgumentException("Applying only 1-size operators")
  }
}

object QbitValue {
  val `|0>` = QbitValue(`1`, `0`)
  val `|1>` = QbitValue(`0`, `1`)
}
