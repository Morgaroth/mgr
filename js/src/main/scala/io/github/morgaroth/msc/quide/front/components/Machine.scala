package io.github.morgaroth.msc.quide.front.components

import io.github.morgaroth.msc.quide.front.api.Api
import io.github.morgaroth.msc.quide.http.CreateCPURes
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.~=>
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
  * Created by mateusz on 10.01.16.
  */
object Machine {

  case class Props(serviceUrl: String, onNewID: CreateCPURes ~=> Callback)

  val component = ReactComponentB[Props]("u")
    .initialState("")
    .render_P { p =>
      <.div(<.button("new", ^.onClick --> Callback {
        Api.createCPU(p.serviceUrl, 5).map { d =>
          p.onNewID(d).runNow()
        }
      }))
    }
    .build

  def apply(url: String, onNewCPU: CreateCPURes ~=> Callback) = component(Props(url, onNewCPU))
}
