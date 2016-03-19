package io.github.morgaroth.msc.quide.core.register

import akka.actor.{ActorRef, Props, Stash}
import io.github.morgaroth.msc.quide.core.actors.QuideActor
import io.github.morgaroth.msc.quide.core.monitoring.CompState.StateAmplitude
import io.github.morgaroth.msc.quide.core.register.QState.{Execute, GateApply, MyAmplitude, ReportValue}
import io.github.morgaroth.msc.quide.model.QValue
import io.github.morgaroth.msc.quide.model.gates.{ControlledGate, Gate, SingleQbitGate}

/**
  * Created by mateusz on 07.03.16.
  */

object QState {

  //@formatter:off
  trait Action
  case class GateApply(operator: Gate, firstQbit: Int) extends Action
  case class ReportValue(to: ActorRef) extends Action
  case class Execute(action: Action, taskNo: Long)
  case class MyAmplitude(ampl: QValue, op: GateApply, taskNo: Long)
  //@formatter:on


  def props(init: QValue = QValue.`0`) = Props(classOf[QState], init)
}

class QState(init: QValue) extends QuideActor with Stash {
  val register = self.path.parent
  val myName = self.path.name
  val deadAmplitude = QValue.`0`

  var amplitude: QValue = init
  var lastNo = -1l

  def loginfo(msg: String) = {
    log.info(s"|$myName> -$msg")
  }

  def ShallIDead() = {
    if (amplitude <= deadAmplitude) {
      context stop self
      true
    } else false
  }

  def executing(gate: SingleQbitGate, myQbit: Char): Receive = {
    case MyAmplitude(ampl, _, _) =>
      loginfo(s"received oppose amplitude $ampl from ${sender()}")
      amplitude = gate.execute(amplitude, ampl, myQbit)
      if (!ShallIDead()) {
        context become receive
        unstashAll()
      } else {
        loginfo("I'm dying...")
      }
    case _ =>
      stash()
  }

  override def receive: Receive = {
    case Execute(o@GateApply(operator: SingleQbitGate, targetBit), no) =>
      loginfo(s"applying 1-qbit operator $operator.(no $no)")
      lastNo = no
      val (myQbit, opposedState) = findOpposedState(targetBit)
      context become executing(operator, myQbit)
      context.actorSelection(register / opposedState) ! MyAmplitude(amplitude, o, no)
    case Execute(o@GateApply(gate: ControlledGate, targetBit), no) =>
      lastNo = no
      if (myName.charAt(myName.length - gate.controlBit - 1) == '0') {
        loginfo("ignoring controlled gate, control bit is 0")
      } else {
        loginfo(s"applying controlled operator $gate.(no $no)")
        val (myQbit, opposedState) = findOpposedState(targetBit)
        context become executing(gate.gate, myQbit)
        context.actorSelection(register / opposedState) ! MyAmplitude(amplitude, o, no)
      }
    case Execute(ReportValue(to), no) =>
      loginfo(s"sending value to reporter.(no $no)")
      to ! StateAmplitude(myName, amplitude, -1)
      lastNo = no

  }

  def findOpposedState(index: Int): (Char, String) = {
    val myQbit: Char = myName(myName.length - index - 1)
    val oponentQbit = if (myQbit == '0') '1' else '0'
    (myQbit, "%s%c%s".format(myName.slice(0, myName.length - index - 1), oponentQbit, myName.slice(myName.length - index, myName.length)))
  }
}
