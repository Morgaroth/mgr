package io.github.morgaroth.msc.quide.front.components

import io.github.morgaroth.msc.quide.http.CreateCPURes
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ReusableFn
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom.ext.KeyCode
import org.scalajs.dom.html.Input

/**
  * Created by mateusz on 09.01.16.
  */
object Root {

  case class State(serviceUrl: String, cpuId: Option[String], cpuSize: Option[Int])

  class Backend($: BackendScope[Unit, State]) {

    def onServiceUrlChanged(e: ReactKeyboardEvent) = {
      CallbackOption.keyCodeSwitch(e) {
        case KeyCode.Enter =>
          val newValue: String = e.currentTarget.asInstanceOf[Input].value.stripSuffix("/")
          println(s"Updating with new service url $newValue")
          $.modState(_.copy(serviceUrl = newValue))
      }
    }

    def newCPU(d: CreateCPURes) = {
      println(s"updating Root state with $d")
      $.modState(_.copy(cpuId = Some(d.id), cpuSize = Some(d.size)))
    }

    def render(state: State) = {
      <.div(
        <.div(<.a("service url: "), <.input(^.defaultValue := state.serviceUrl, ^.onKeyDown ==> onServiceUrlChanged)),
        Machine(state.serviceUrl, ReusableFn(newCPU)),
        (state.cpuId, state.cpuSize) match {
          case (Some(cpuId), Some(cpuSize)) => CPUControls(state.serviceUrl, cpuSize, cpuId)
          case _ => <.p("No information about selected cpu, maybe create new, or select existing?")
        }
      )
    }
  }

  val component = ReactComponentB[Unit]("root")
    .initialState(State("http://localhost:9999", None, None))
    .renderBackend[Backend]
    .buildU

  def apply() = component()
}