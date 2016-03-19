package io.github.morgaroth.msc.quide.front.components

import io.github.morgaroth.msc.quide.model.gates.SingleQbitGate
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

    case class Props(available: List[SingleQbitGate], cur: Option[SingleQbitGate], controlled: Boolean, change: SingleQbitGate ~=> Callback)

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

    case class Props(size: Int, controlledOn: Option[Int] = None, targetBit: Option[Int], controlBit: Option[Int] ~=> Callback)

    val component = ReactComponentB[Props]("controlInput")
      .render_P { s =>
        <.div(^.id := "menu",
          <.b(s"Controlled? (current: ${s.controlledOn.map(x => s"by $x").getOrElse("no")}):"),
          <.br,
          <.button("Simple", ^.onClick --> s.controlBit(None)),
          <.ul(
            (0 until s.size).filterNot(s.targetBit.contains) map { x =>
              <.button(x.toString, ^.onClick --> s.controlBit(Some(x)))
            }
          )
        )
      }
      .build
  }

  case class State(gate: Option[SingleQbitGate] = None, controlledTo: Option[Int] = None, qbit: Option[Int] = None)

  case class Props(available: List[SingleQbitGate], registerSize: Int, execute: (SingleQbitGate, Int, Option[Int]) => Callback)

  class Backend($: BackendScope[Props, State]) {

    def setGate(o: SingleQbitGate): Callback = {
      println(s"updating gate to $o")
      $.modState(_.copy(gate = Some(o)))
    }

    def controlled(newValue: Option[Int]): Callback = {
      println(s"updating controlBit to $newValue")
      $.modState(_.copy(controlledTo = newValue))
    }

    def setFirstQbit(q: Int): Callback = {
      println(s"updating tagetBit to $q")
      $.modState(_.copy(qbit = Some(q)))
    }

    def onDone(props: Props) = {
      $.state.flatMap { s =>
        println(s"triggering execute of gate with state $s")
        props.execute(s.gate.get, s.qbit.get, s.controlledTo)
      }
    }

    def render(s: State, props: Props) = {
      println(s"rerendering actioner with state $s and props $props")
      if (s.gate.isDefined && s.qbit.isDefined) {
        <.div(
          OperatorSelector(OperatorSelector.Props(props.available, s.gate, s.controlledTo.nonEmpty, ReusableFn(setGate))),
          <.br,
          IndexSelector(IndexSelector.Props(props.registerSize, s.qbit, ReusableFn(setFirstQbit))),
          <.br,
          ControlledSelector(ControlledSelector.Props(props.registerSize, s.controlledTo, s.qbit, ReusableFn(controlled))),
          <.br,
          <.button(s"Execute", ^.onClick --> onDone(props)), <.a(s" operator ${s.controlledTo.map(_ => "C").getOrElse("")}${s.gate.get} on qbit ${s.qbit.get.toString}${s.controlledTo.map(x => s" controlled by   $x").getOrElse("")}")
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
        gate = a.$.state.gate orElse a.nextProps.available.headOption,
        qbit = a.$.state.qbit orElse (if (a.nextProps.registerSize > 0) Some(0) else None)
      ))
    )
    .build

  def apply(
             available: List[SingleQbitGate],
             registerSize: Int,
             execute: (SingleQbitGate, Int, Option[Int]) => Callback) =
    component(Props(available, registerSize, execute))

}
