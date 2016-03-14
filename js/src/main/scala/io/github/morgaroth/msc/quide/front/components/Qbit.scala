package io.github.morgaroth.msc.quide.front.components

import io.github.morgaroth.msc.quide.model.QValue
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by mateusz on 08.01.16.
  */
object Qbit {

  val component = ReactComponentB[(String, QValue)]("Qbit")
    .render_P { d =>
      <.div(
        "<",<.b(d._1), "| — ", Complex(d._2)
      )
    }.build

  def apply(a: (String, QValue)) = component(a)
}
