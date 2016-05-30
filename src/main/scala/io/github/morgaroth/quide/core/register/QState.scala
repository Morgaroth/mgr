package io.github.morgaroth.quide.core.register

import akka.actor.ActorRef
import io.github.morgaroth.quide.core.model.QValue
import io.github.morgaroth.quide.core.model.gates.Gate

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
  case object Ping extends Action
  case object Busy extends Action
  case class MyAmplitude(ampl: QValue, op: GateApply, taskNo: Long)
  //@formatter:on

}