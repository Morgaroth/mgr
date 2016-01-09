package io.github.morgaroth.msc.quide.front

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by mateusz on 08.01.16.
  */
object Actioner {

  object IndexSelector {
    def apply(state: Props) = component(state)

    case class Props(size: Int, cur: Option[Int], change: Int ~=> Callback)

    implicit val inputControlReuse = Reusability.caseClass[Props]

    val component = ReactComponentB[Props]("indexControl")
      .render_P { p =>
        p.cur.map[ReactElement](c => <.div(^.id := "menu",
          <.p(<.b(s"Chose index (current: $c):")),
          <.ul(
            0 until p.size map { x =>
              <.button(x.toString, ^.onClick --> p.change(x))
            }
          )
        )).getOrElse(<.p("no information about register size, no qbits :("))
      }
      .build
  }

  object OperatorSelector {
    def apply(state: Props) = component(state)

    case class Props(available: List[String], cur: Option[String], change: String ~=> Callback)

    implicit val inputControlReuse = Reusability.caseClass[Props]

    val component = ReactComponentB[Props]("operatorInput")
      .render_P { s =>
        <.div(^.id := "menu",
          <.p(<.b(s"Chose operator (current: ${s.cur.get}):")),
          <.ul(
            s.available map { x =>
              <.button(
                x,
                ^.onClick --> s.change(x)
              )
            }
          )
        )
      }
      .build
  }

  case class State(operator: Option[String] = None, qbit: Option[Int] = None)

  case class Props(available: List[String], registerSize: Int)

  class Backend($: BackendScope[Props, State]) {

    def setOperator(o: String): Callback = {
      println(s"updteing operator  to $o")
      $.modState(_.copy(operator = Some(o)))
    }

    def setFirstQbit(q: Int): Callback = {
      println(s"updating index to $q")
      $.modState(_.copy(qbit = Some(q)))
    }

    def onDone(state: State) = {
      Callback {
        println(s"setting with state $state")
      }
    }

    //    def componentWillReceiveProps(props:Props) = {
    //      println(s"new propss $props")
    //    }

    def render(state: State, props: Props) = {
      println(s"rerendering actioner with state $state and props $props")
      <.div(
        if (props.available.nonEmpty) OperatorSelector(OperatorSelector.Props(props.available, state.operator, ReusableFn(setOperator))) else <.p("Wait for available operators..."),
        <.br,
        if (props.registerSize > 0) IndexSelector(IndexSelector.Props(props.registerSize, state.qbit, ReusableFn(setFirstQbit))) else <.p("Set up register size"),
        <.br,
        <.button(s"Execute", ^.onClick --> onDone(state)), <.a(s" operator ${state.operator.getOrElse("")} from qbit ${state.qbit.map(_.toString).getOrElse("")}")
      )
    }
  }

  val component = ReactComponentB[Props]("input")
    .initialState_P(p => {
      println("initializing actioner")
      State(p.available.headOption, if (p.registerSize > 0) Some(0) else None)
    })
    .renderBackend[Backend]
    .componentWillReceiveProps(a =>
      a.$.modState(_.copy(
        operator = a.$.state.operator orElse a.nextProps.available.headOption,
        qbit = a.$.state.qbit orElse (if (a.nextProps.registerSize > 0) Some(0) else None)
      ))
    )
    .build

  //  val component = ReactComponentB[Unit]("input")

  def apply(props: Props) = component(props)

}
