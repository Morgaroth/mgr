package io.github.morgaroth.msc.quide.front.components

import io.github.morgaroth.msc.quide.front.api.Api
import io.github.morgaroth.msc.quide.model.QValue
import io.github.morgaroth.msc.quide.model.gates.{ControlledGate, SingleQbitGate}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by mateusz on 09.01.16.
  */
object CPUControls {

  case class Props(url: String, cpuSize: Int, cpuId: String)

  case class State(
                    operators: List[SingleQbitGate],
                    register: List[(String, QValue)],
                    lastOperation: Int
                  ) {
    def size = register.size
  }

  class Backend($: BackendScope[Props, State]) {

    def updateRegister(lastNo: Int, newValue: List[(String, QValue)]) = {
      $.modState(_.copy(register = newValue, lastOperation = lastNo))
    }

    def fetchRegisterState = {
      $.props.flatMap(p => Callback {
        Api.getCPUState(p.url, p.cpuId).map { d =>
          println(s"state of ${p.cpuId} is $d")
          $.modState(_.copy(register = d.toList.sortBy(_._1))).runNow()
        }
      })
    }

    def executeOperator(o: SingleQbitGate, i: Int, controlled: Option[Int]): Callback = {
      $.props.flatMap(p => Callback {
        val task = controlled.map(controlBit => ControlledGate(o, controlBit)).getOrElse(o)
        Api.executeOperator(p.url, p.cpuId, task, i).map { response =>
          println(s"state of ${p.cpuId} is $response")
          $.modState(_.copy(register = response.toList.sortBy(_._1))).runNow()
        }
      })
    }

    def render(state: State, props: Props) = {
      println(s"rerendering cpu controls with state $state and props $props")
      <.div(
        CompState(state.register),
        <.br,
        CompActions(state.operators, props.cpuSize, executeOperator)
      )
    }
  }

  val component = ReactComponentB[Props]("panel22")
    .initialState(State(List.empty, List.empty, -1))
    .renderBackend[Backend]
    .componentDidMount { f => init(f.props, f.modState(_: State => State)) }
    .componentWillReceiveProps { f => init(f.nextProps, f.$.modState(_: State => State)) }
    .build

  def init(p: Props, mod: ((State) => State) => Callback): CallbackTo[Unit] = {
    Callback {
      Api.getOperatorList(p.url).map { operators =>
        println(s"new list of operators fetched from backend $operators")
        mod(_.copy(operators = operators)).runNow()
      }
    } flatMap { _ =>
      Callback {
        Api.getCPUState(p.url, p.cpuId).map { d =>
          println(s"state of ${p.cpuId} is $d")
          mod(_.copy(register = d.toList.sortBy(_._1))).runNow()
        }
      }
    }
  }

  def apply(serviceUrl: String, cpuSize: Int, cpuId: String) = component(Props(serviceUrl, cpuSize, cpuId))
}
