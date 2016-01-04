package io.github.morgaroth.msc.quide.core.model

/**
  * Created by mateusz on 04.01.16.
  */
case class QbitValue(a: `|0>`, b: `|1>`) {
  def apply(o: Operator) = {
    if (o.size == 1) {
      QbitValue(o(0, 0) * a + o(0, 1) * b, o(1, 0) * a + o(1, 1) * b)
    } else throw new IllegalArgumentException("Applying only 1-size operators")
  }
}
