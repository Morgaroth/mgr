package io.github.morgaroth.msc.quide.front

import io.github.morgaroth.msc.quide.front.components.{Machine, Root}
import japgolly.scalajs.react._
import org.scalajs.dom

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport
import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scalacss.mutable.GlobalRegistry

object Main extends JSApp {

  @JSExport
  override def main(): Unit = {
    MainCSS.load()
    dom.window.location.href
    val mounted = ReactDOM.render(Root(dom.window.location.href), dom.document.getElementsByClassName("quide")(0))
  }
}


object MainCSS {

  def load() = {
    GlobalRegistry.register(Machine.DefaultStyle)
    GlobalRegistry.addToDocumentOnRegistration()
  }
}