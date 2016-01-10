package io.github.morgaroth.msc.quide.front.components

import io.github.morgaroth.msc.quide.model.QbitValue
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by mateusz on 08.01.16.
  */
object Qbit {

  val component = ReactComponentB[QbitValue]("Qbit")
    .render_P { d =>
      <.div(
        Complex(d.a_0),
        <.br,
        Complex(d.b_1)
      )
    }.build

  def apply(a: QbitValue) = component(a)
}
