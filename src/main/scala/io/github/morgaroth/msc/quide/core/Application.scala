package io.github.morgaroth.msc.quide.core

import akka.event.Logging
import akka.io.IO
import com.typesafe.config.ConfigFactory
import io.github.morgaroth.msc.quide.core.server.{Core, BootedCore, WebApi, CoreActors}
import spray.can.Http
import net.ceedubs.ficus.Ficus._

/**
  * Created by mateusz on 03.01.16.
  */
trait Web {
  this: WebApi with CoreActors with Core =>

  val log = Logging(system, "web")

  val port = {
    val port = ConfigFactory.load().as[Int]("port")
    log.info(s"Binding to port $port...")
    port
  }

  IO(Http)(system) ! Http.Bind(rootService, "0.0.0.0", port = port)
}

object Application extends App with BootedCore with CoreActors with WebApi with Web