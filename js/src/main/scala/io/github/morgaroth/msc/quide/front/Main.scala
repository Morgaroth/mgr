package io.github.morgaroth.msc.quide.front

import io.github.morgaroth.msc.quide.front.components.Root
import japgolly.scalajs.react._
import org.scalajs.dom

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

object Main extends JSApp {
  //  val baseUrl = BaseUrl(dom.window.location.href.takeWhile(_ != '#'))
  //
  //  val routerConfig: RouterConfig[ModeFilter] = RouterConfigDsl[ModeFilter].buildConfig { dsl =>
  //    import dsl._
  //
  //    /* how the application renders the list given a filter */
  //    def filterRoute(s: ModeFilter): Rule = staticRoute("#/" + s.link, s) ~> renderR(s.component(s))
  //
  //    val filterRoutes: Rule = ModeFilter.values.map(filterRoute).reduce(_ | _)
  //
  //    /* build a final RouterConfig with a default page */
  //    filterRoutes.notFound(redirectToPage(ModeFilter.Reusability)(Redirect.Replace))
  //  }
  //
  //  /** The router is itself a React component, which at this point is not mounted (U-suffix) */
  //  val router: ReactComponentU[Unit, Resolution[ModeFilter], Any, TopNode] =
  //    Router(baseUrl, routerConfig.logToConsole)()

  /**
    * Main entry point, which the sbt plugin finds and makes the browser run.
    *
    * Takes the unmounted router component and gives to React,
    * will render into the first element with `todoapp` class
    */
  @JSExport
  override def main(): Unit = {
    val mounted = ReactDOM.render(Root(), dom.document.getElementsByClassName("quide")(0))
  }
}
