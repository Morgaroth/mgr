package io.github.morgaroth.msc.quide.front.components

import io.github.morgaroth.msc.quide.model
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by mateusz on 08.01.16.
  */
object Complex {

  val component = ReactComponentB[model.Complex]("Complex")
    .render_P {
      case model.Complex(re, 0d) =>
        <.a(<.b(pretty("%.3f".format(re))))
      case model.Complex(0d, im) =>
        <.a(<.b(pretty("%.3f".format(im))), "i")
      case model.Complex(re, im) =>
        <.a(<.b(pretty("%.3f".format(re))), " + ", <.b(pretty("%.3f".format(im))), "i")
    }.build

  def apply(d: model.Complex) = component(d)

  def pretty(number:String) = {
    val decimalPoints = Set(',','.')
    number.view.reverse.dropWhile(_ == '0').dropWhile(decimalPoints.contains).reverse.mkString
  }
}
