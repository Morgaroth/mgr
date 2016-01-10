package io.github.morgaroth.msc.quide.front.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by mateusz on 08.01.16.
  */
object CompActions {

  object IndexSelector {
    def apply(state: Props) = component(state)

    case class Props(size: Int, cur: Option[Int], change: Int ~=> Callback)

    implicit val inputControlReuse = Reusability.caseClass[Props]

    val component = ReactComponentB[Props]("indexControl")
      .render_P { p =>
        p.cur.map[ReactElement](c => <.div(^.id := "menu",
          <.b(s"Chose index (current: $c):"),
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
          <.b(s"Chose operator (current: ${s.cur.get}):"),
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

  case class Props(available: List[String], registerSize: Int, execute: (String, Int) => Callback)

  class Backend($: BackendScope[Props, State]) {

    def setOperator(o: String): Callback = {
      println(s"updteing operator  to $o")
      $.modState(_.copy(operator = Some(o)))
    }

    def setFirstQbit(q: Int): Callback = {
      println(s"updating index to $q")
      $.modState(_.copy(qbit = Some(q)))
    }

    def onDone(props: Props) = {
      $.state.flatMap { s =>
        println(s"triggering execute of operator with state $s")
        props.execute(s.operator.get, s.qbit.get)
      }
    }

    def render(state: State, props: Props) = {
      println(s"rerendering actioner with state $state and props $props")
      if (state.operator.isDefined && state.qbit.isDefined) {
        <.div(
          OperatorSelector(OperatorSelector.Props(props.available, state.operator, ReusableFn(setOperator))),
          <.br,
          IndexSelector(IndexSelector.Props(props.registerSize, state.qbit, ReusableFn(setFirstQbit))),
          <.br,
          <.button(s"Execute", ^.onClick --> onDone(props)), <.a(s" operator ${state.operator.get} from qbit ${state.qbit.get.toString}")
        )
      } else {
        <.p(s"state $state is empty")
      }
    }
  }

  val component = ReactComponentB[Props]("actions-panel")
    .initialState_P(p => {
      println("initializing actions panel")
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

  def apply(
             available: List[String],
             registerSize: Int,
             execute: (String, Int) => Callback) =
    component(Props(available, registerSize, execute))

}
