package io.github.morgaroth.msc.quide.front.components

import io.github.morgaroth.msc.quide.model.QbitValue
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by mateusz on 09.01.16.
  */
object CompState {
  val component = ReactComponentB[List[QbitValue]]("CompState")
    .render_P { p =>
      <.div(^.id := "menu",
        <.p(<.b("CPU State:")),
        if (p.isEmpty) <.p("Wait for data....")
        else {
          <.ul(
            p.map(x =>
              <.li(Qbit(x)))
          )
        }
      )
    }
    .build

  def apply(qbits: List[QbitValue]) = component(qbits)
}
