package io.github.morgaroth.msc.quide.core.register

import akka.actor.{ActorRef, DeadLetter, Props}
import io.github.morgaroth.msc.quide.core.actors.QuideActor
import io.github.morgaroth.msc.quide.core.register.QState.{Execute, GateApply, Ready}
import io.github.morgaroth.msc.quide.core.register.Register.{ExecuteGate, ReportValue}
import io.github.morgaroth.msc.quide.model.QValue
import io.github.morgaroth.msc.quide.model.gates.Gate

/**
  * Created by mateusz on 03.01.16.
  */
object Register {
  //@formatter:off
  case class Step()
  case class ExecuteGate(gate: Gate, qbit: Int)
  case class ReportValue(to: ActorRef)
  //@formatter:on
}