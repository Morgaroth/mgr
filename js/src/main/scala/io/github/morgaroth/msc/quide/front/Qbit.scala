package io.github.morgaroth.msc.quide.front

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by mateusz on 08.01.16.
  */
object Qbit {

  case class State(a: Complex.State, b: Complex.State)

  val component = ReactComponentB[State]("Qbit")
    .render_P { d =>
      <.div(
        Complex(d.a),
        <.br,
        Complex(d.b)
      )
    }.build

  def apply(a: State) = component(a)
}
