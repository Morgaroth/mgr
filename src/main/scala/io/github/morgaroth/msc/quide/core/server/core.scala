package io.github.morgaroth.msc.quide.core.server

import akka.actor.ActorSystem

trait Core {
  implicit def system: ActorSystem
}

trait BootedCore extends Core {
  implicit lazy val system = ActorSystem("roulette-backend")
  sys.addShutdownHook(system.terminate())
}

trait CoreActors {
  this: Core =>

  // actors
  // val sessionsActor = system.actorOf(Props[SessionAliveActor])
}