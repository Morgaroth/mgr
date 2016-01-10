package io.github.morgaroth.msc.quide.front.components

import io.github.morgaroth.msc.quide.model
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by mateusz on 08.01.16.
  */
object Complex {

  val component = ReactComponentB[model.Complex]("Complex")
    .render_P { data =>
      <.a(data.toString())
    }.build

  def apply(d: model.Complex) = component(d)
}
