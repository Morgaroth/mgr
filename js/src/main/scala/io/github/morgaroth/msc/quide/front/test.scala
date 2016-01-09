package io.github.morgaroth.msc.quide.front

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom.ext.Ajax
import upickle._
import upickle.default._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow


object test {

  val CompState = ReactComponentB[List[Qbit.State]]("CompState")
    .render_P { p =>
      <.div(^.id := "menu",
        <.p(<.b("CPU State:")),
        if (p.isEmpty) <.p("Wait for data....")
        else {
          <.ul(
            p.map(x =>
              <.li(Qbit.apply(x)))
          )
        }
      )
    }
    .build

  object Root {

    case class State(
                      operators: List[String],
                      register: List[Qbit.State],
                      lastOperation: Int
                    ) {
      def size = register.size
    }


    val url = "http://localhost:9999/cpu/operators"

    class Backend($: BackendScope[Unit, State]) {

      def updateRegister(lastNo: Int, newValue: List[Qbit.State]) = {
        $.modState(_.copy(register = newValue, lastOperation = lastNo))
      }

      //      def fetchOperators(): Callback = {
      //      }

      def render(state: State) = {
        println(s"rerendering root with state $state")
        <.div(
          CompState(state.register),
          <.br,
          Actioner.component(Actioner.Props(state.operators, state.size))
        )
      }

    }

    val component = ReactComponentB[Unit]("panel22")
      .initialState(State(List.empty, List.empty, -1))
      .renderBackend[Backend]
      .componentDidMount {
        c => Callback {
          Ajax.get(url).map(_.responseText).map { d =>
            val operators = read[List[String]](d)
            println(s"new list of operators fetched from backend $operators")
            c.modState(_.copy(operators = operators)).runNow()
          }
        }
      }
      .buildU


  }

  val testCompState = Root.component()
}
