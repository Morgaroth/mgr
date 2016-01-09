package io.github.morgaroth.msc.quide.front

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by mateusz on 08.01.16.
  */
object Complex {

  case class State(re: Double, im: Double)

  val component = ReactComponentB[State]("Complex")
    .render_P { pa =>
      <.a(s"${pa.re}+${pa.im}i")
    }.build

  def apply(d: State): ReactComponentU[State, Unit, Unit, TopNode] = component(d)

  def apply(d: (Double, Double)): ReactComponentU[State, Unit, Unit, TopNode] = apply(State(d._1, d._2))
}
