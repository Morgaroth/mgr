package io.github.morgaroth.msc.quide.front.components

import chandu0101.scalajs.react.components.ReactListView
import io.github.morgaroth.msc.quide.front.api.Api
import io.github.morgaroth.msc.quide.http.CPU
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.~=>
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.language.postfixOps
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue


/**
  * Created by mateusz on 10.01.16.
  */

object Machine {

  case class Props(serviceUrl: String, useCPU: CPU ~=> Callback)

  case class State(availableCPUS: List[CPU] = List.empty, size: Int = 1, selectedCPU: Option[CPU] = None) {
    def avaliableNotSelected = selectedCPU.map(x => availableCPUS.filterNot(_ == x)).getOrElse(availableCPUS)
  }

  class Backend($: BackendScope[Props, State]) {

    def refresh: CallbackTo[Unit] = {
      $.props.flatMap(p => Callback {
        Api.listCPUs(p.serviceUrl).map { d =>
          $.modState(_.copy(availableCPUS = d)).runNow()
        }
      })
    }

    val onCPUSelected = (item: String) => Callback {
      //      dom.document.getElementById("listviewcontent").innerHTML = s"Selected CPU: $item <br>"
      $.props.flatMap { props =>
        $.state.flatMap { state =>
          val id = item.takeWhile(_ != ',')
          val cpu = state.availableCPUS.find(_.id == id).get
          $.modState(_.copy(selectedCPU = Some(cpu))).flatMap(_ => props.useCPU(cpu))
        }
      }.runNow()
    }

    val onCpuSizeSelected = (item: String) => {
      Callback($.modState(_.copy(size = item.takeWhile(_ != ' ').toInt)).runNow())
    }

    def createNew: Callback = {
      $.props.flatMap { p =>
        $.state.flatMap(s => Callback {
          Api.createCPU(p.serviceUrl, s.size).map { d =>
            $.modState(_.copy(selectedCPU = Some(d))).flatMap(_ => p.useCPU(d))
          }
        })
      }.flatMap(_ => refresh)
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
            ),
            <.button(s"new ${s.size}qbits processor", ^.onClick --> createNew),
            <.button("refresh", ^.onClick --> refresh)
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

  val component = ReactComponentB[Props]("u")
    .initialState(State())
    .renderBackend[Backend]
    .componentDidMount { f => init(f.props, f.modState(_: State => State)) }
    .componentWillReceiveProps { f => init(f.nextProps, f.$.modState(_: State => State)) }
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
