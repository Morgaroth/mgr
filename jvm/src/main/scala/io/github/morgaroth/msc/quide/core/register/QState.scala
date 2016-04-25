package io.github.morgaroth.msc.quide.core.register

import akka.actor.{ActorRef, Props, Stash}
import io.github.morgaroth.msc.quide.core.actors.QuideActor
import io.github.morgaroth.msc.quide.core.monitoring.CompState.StateAmplitude
import io.github.morgaroth.msc.quide.core.register.QState._
import io.github.morgaroth.msc.quide.model.QValue
import io.github.morgaroth.msc.quide.model.gates.{Gate, MultiControlledGate, SingleQbitGate}

/**
  * Created by mateusz on 07.03.16.
  */

object QState {

  //@formatter:off
  trait Action
  case class GateApply(operator: Gate, firstQbit: Int) extends Action
  case class ReportValue(to: ActorRef) extends Action
  case class Execute(action: Action, taskNo: Long)
  case object Ready extends Action
  case class MyAmplitude(ampl: QValue, op: GateApply, taskNo: Long)
  //@formatter:on

}