package io.github.morgaroth.quide.core.register

import akka.actor.{ActorPath, ActorRef}
import io.github.morgaroth.quide.core.model.gates.Gate

/**
  * Created by mateusz on 03.01.16.
  */
object Register {

  //@formatter:off
  case class Step()
  case class ExecuteGate(gate: Gate, qbit: Int)
  case class ReportValue(to: ActorRef)
  //@formatter:on

  case class ImDied(path: ActorPath)

}