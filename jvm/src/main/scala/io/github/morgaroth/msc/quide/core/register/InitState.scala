package io.github.morgaroth.msc.quide.core.register

import io.github.morgaroth.msc.quide.model.QValue

/**
  * Created by morgaroth on 22.04.2016.
  */
case class InitState(name: String, value: QValue = QValue.`1`) {
  val valid: Set[Char] = Set('1', '0')
  assert(name.forall(valid.contains))
}

