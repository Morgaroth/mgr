package io.github.morgaroth.msc.quide.front.components

import chandu0101.scalajs.react.components.ReactListView
import io.github.morgaroth.msc.quide.front.api.Api
import io.github.morgaroth.msc.quide.http.CPU
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.~=>
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.concurrent.Future
import scala.language.postfixOps
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue


/**
  * Created by mateusz on 10.01.16.
  */

object Machine {

  case class Props(serviceUrl: String, useCPU: CPU ~=> Callback)

  case class State(availableCPUS: List[CPU] = List.empty, size: Int = 1, selectedCPU: Option[CPU] = None, refreshNeed: Boolean = false) {
    def avaliableNotSelected = selectedCPU.map(x => availableCPUS.filterNot(_ == x)).getOrElse(availableCPUS)
  }

  class Backend($: BackendScope[Props, State]) {

    def p_s: CallbackTo[(Props, State)] = $.props zip $.state

    def refresh(s: State, p: Props) = Callback {
      println(s"Refreshing list with local $s and $p")
      Api.listCPUs(p.serviceUrl).map { d =>
        $.modState(s => s.copy(availableCPUS = d, selectedCPU = if (d.isEmpty) None else s.selectedCPU))
      }
    }

    def refresh: CallbackTo[Unit] = {
      println(s"Refreshing list with local state and props")
      Callback($.props.flatMap(p => Callback {
        Api.listCPUs(p.serviceUrl).map { d =>
          $.modState(s => s.copy(availableCPUS = d, selectedCPU = if (d.isEmpty) None else s.selectedCPU))
        }
      }).runNow())
    }

    val onCPUSelected = (item: String) => {
      $.props.flatMap { props =>
        $.modState { state =>
          val id = item.takeWhile(_ != ',')
          val cpu = state.availableCPUS.find(_.id == id).get
          state.copy(selectedCPU = Some(cpu))
        }
      }
    }

    val onCpuSizeSelected = (item: String) => {
      $.modState(_.copy(size = item.takeWhile(_ != ' ').toInt))
    }

    def createNew: Callback = p_s.flatMap {
      case (props, state) => Callback.future {
        createAndGetList(props, state) map {
          case (created: CPU, newlist: List[CPU]) =>
            $.modState(_.copy(availableCPUS = newlist, selectedCPU = Some(created)), props.useCPU(created))
        }
      }
    }

    def createAndGetList(p: Props, s: State): Future[(CPU, List[CPU])] = {
      Api.createCPU(p.serviceUrl, s.size) flatMap (x => Api.listCPUs(p.serviceUrl).map(x -> _))
    }

    def render(p: Props, s: State) = {
      println(s"rerender machine with $s and $p")
      <.div(
        <.div(
          ^.display := "inline-flex",
          <.div(
            <.strong("New processor size:"),
            ReactListView(
              items = 1 to 20 map (x => s"$x qbits") toList,
              showSearchBox = false,
              onItemSelect = onCpuSizeSelected,
              style = DefaultStyle
            )
          ),
          <.div(
            <.strong("Available CPUs:"),
            ReactListView(
              items = s.availableCPUS.map(x => s"${x.id}, ${x.size}qbits"),
              showSearchBox = false,
              onItemSelect = onCPUSelected,
              style = DefaultStyle
            )
          )
        ),
        <.hr,
        <.button(s"new ${s.size}qbits processor", ^.onClick --> createNew),
        <.button("refresh", ^.onClick --> refresh),
        <.br, <.br,
        <.strong(
          ^.id := "listviewcontent",
          s.selectedCPU.map[TagMod](x => s"Selected CPU: ${x.id}, ${x.size}qbits").getOrElse("Selected Content Here")
        )
      )
    }
  }

  def init(p: Props, mod: ((State) => State) => Callback): CallbackTo[Unit] = Callback {
    Api.listCPUs(p.serviceUrl).map { d =>
      mod(s => s.copy(availableCPUS = d, selectedCPU = s.selectedCPU orElse d.headOption)).runNow()
    }
  }

  val whenUpdate: (ComponentWillUpdate[Props, State, Backend, TopNode]) => Callback = {
    f =>
      Callback {
        println("when update:")
        println(s"     f.$$.state = ${f.$.state}")
        println(s"     f.$$.props = ${f.$.props}")
        println(s"     f.$$.nextState = ${f.nextState}")
        println(s"     f.$$.nextProps = ${f.nextProps}")
      }
  }

  val component = ReactComponentB[Props]("u")
    .initialState(State())
    .renderBackend[Backend]
    .componentDidMount { f => init(f.props, f.modState(_: State => State)) }
    //    .componentWillReceiveProps { f => init(f.nextProps, f.$.modState(_: State => State)) }
    //    .componentWillUpdate(whenUpdate)
    .build

  def apply(url: String, useCPU: CPU ~=> Callback) = component(Props(url, useCPU))

  class Style extends ReactListView.Style {

    import dsl._

    import scalacss.Defaults._

    override val listGroup = style(marginBottom(20.px),
      paddingLeft.`0`,
      &.firstChild.lastChild(borderBottomLeftRadius(4 px),
        borderBottomRightRadius(4 px))
    )

    override val listItem = styleF.bool(selected => styleS(position.relative,
      display.block,
      padding(v = 10.px, h = 15.px),
      border :=! "1px solid #ecf0f1",
      cursor.pointer,
      mixinIfElse(selected)(color.white,
        fontWeight._500,
        backgroundColor :=! "#146699")(
        backgroundColor.white,
        &.hover(color :=! "#555555",
          backgroundColor :=! "#ecf0f1"))
    ))
  }

  object DefaultStyle extends Style

}
