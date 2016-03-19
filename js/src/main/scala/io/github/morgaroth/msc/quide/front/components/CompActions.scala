package io.github.morgaroth.msc.quide.front.components

import io.github.morgaroth.msc.quide.model.operators.SingleQbitOperator
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

    case class Props(available: List[SingleQbitOperator], cur: Option[SingleQbitOperator], controlled: Boolean, change: SingleQbitOperator ~=> Callback)

    val component = ReactComponentB[Props]("operatorInput")
      .render_P { s =>
        <.div(^.id := "menu",
          <.b(s"Chose operator (current: ${if (s.controlled) "C" else ""}${s.cur.get}):"),
          <.ul(
            s.available map { x =>
              <.button(
                x.toString,
                ^.onClick --> s.change(x)
              )
            }
          )
        )
      }
      .build
  }

  object ControlledSelector {
    def apply(state: Props) = component(state)

    case class Props(size: Int, controlledOn: Option[Int] = None, currentFrom: Option[Int], controlTo: Option[Int] ~=> Callback)

    val component = ReactComponentB[Props]("controlInput")
      .render_P { s =>
        <.div(^.id := "menu",
          <.b(s"Controlled? (current: ${s.controlledOn.map(x => s"by $x").getOrElse("no")}):"),
          <.br,
          <.button("Simple", ^.onClick --> s.controlTo(None)),
          <.ul(
            (0 until s.size).filterNot(s.currentFrom.contains) map { x =>
              <.button(x.toString, ^.onClick --> s.controlTo(Some(x)))
            }
          )
        )
      }
      .build
  }

  case class State(operator: Option[SingleQbitOperator] = None, controlledTo: Option[Int] = None, qbit: Option[Int] = None)

  case class Props(available: List[SingleQbitOperator], registerSize: Int, execute: (SingleQbitOperator, Int, Option[Int]) => Callback)

  class Backend($: BackendScope[Props, State]) {

    def setOperator(o: SingleQbitOperator): Callback = {
      println(s"updteing operator  to $o")
      $.modState(_.copy(operator = Some(o)))
    }

    def controlled(newValue: Option[Int]): Callback = {
      println(s"updating negated to $newValue")
      $.modState(_.copy(controlledTo = newValue))
    }

    def setFirstQbit(q: Int): Callback = {
      println(s"updating index to $q")
      $.modState(_.copy(qbit = Some(q)))
    }

    def onDone(props: Props) = {
      $.state.flatMap { s =>
        println(s"triggering execute of operator with state $s")
        props.execute(s.operator.get, s.qbit.get, s.controlledTo)
      }
    }

    def render(s: State, props: Props) = {
      println(s"rerendering actioner with state $s and props $props")
      if (s.operator.isDefined && s.qbit.isDefined) {
        <.div(
          OperatorSelector(OperatorSelector.Props(props.available, s.operator, s.controlledTo.nonEmpty, ReusableFn(setOperator))),
          <.br,
          IndexSelector(IndexSelector.Props(props.registerSize, s.qbit, ReusableFn(setFirstQbit))),
          <.br,
          ControlledSelector(ControlledSelector.Props(props.registerSize, s.controlledTo, s.qbit, ReusableFn(controlled))),
          <.br,
          <.button(s"Execute", ^.onClick --> onDone(props)), <.a(s" operator ${s.controlledTo.map(_ => "C").getOrElse("")}${s.operator.get} on qbit ${s.qbit.get.toString}${s.controlledTo.map(x => s" controlled by   $x").getOrElse("")}")
        )
      } else {
        <.p(s"state $s is empty")
      }
    }
  }

  val component = ReactComponentB[Props]("actions-panel")
    .initialState_P(p => {
      println("initializing actions panel")
      State(p.available.headOption, controlledTo = None, if (p.registerSize > 0) Some(0) else None)
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
             available: List[SingleQbitOperator],
             registerSize: Int,
             execute: (SingleQbitOperator, Int, Option[Int]) => Callback) =
    component(Props(available, registerSize, execute))

}
