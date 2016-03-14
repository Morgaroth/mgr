package io.github.morgaroth.msc.quide.front.components

import io.github.morgaroth.msc.quide.http.CPU
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ReusableFn
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by mateusz on 09.01.16.
  */
object Root {

  case class State(serviceUrl: String, cpuId: Option[String], cpuSize: Option[Int])

  class Backend($: BackendScope[_, State]) {

    def newCPU(d: CPU) = {
      println(s"updating Root state with $d")
      $.modState(_.copy(cpuId = Some(d.id), cpuSize = Some(d.size)))
    }

    def setServiceAddressTo(str: String): Callback = {
      println(s"updating state to $str")
      $.modState(_.copy(serviceUrl = str))
    }

    def render(state: State) = {
      <.div(
        <.div(
          <.a("service url: ", <.b(state.serviceUrl)),
          <.br,
          <.button("localhost", ^.onClick --> setServiceAddressTo("http://localhost:9999")),
          <.button("jaje.ninja", ^.onClick --> setServiceAddressTo("http://api.quide.jaje.ninja"))
        ),
        Machine(state.serviceUrl, ReusableFn(newCPU)),
        <.br,
        (state.cpuId, state.cpuSize) match {
          case (Some(cpuId), Some(cpuSize)) => CPUControls(state.serviceUrl, cpuSize, cpuId)
          case _ => <.p("No information about selected cpu, maybe create new, or select existing?")
        }
      )
    }
  }

  val component = ReactComponentB[Unit]("root")
    //    .initialState(State("http://api.quide.jaje.ninja", None, None))
    .initialState(State("http://localhost:9999", None, None))
    .renderBackend[Backend]
    .buildU

  def apply() = component()
}