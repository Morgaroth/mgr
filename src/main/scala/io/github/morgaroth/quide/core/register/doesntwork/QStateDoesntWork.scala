package io.github.morgaroth.quide.core.register.doesntwork

import akka.actor.{Props, Stash}
import io.github.morgaroth.quide.core.actors.QStateActor
import io.github.morgaroth.quide.core.model.QValue
import io.github.morgaroth.quide.core.model.gates.{ControlledGate, SingleQbitGate}
import io.github.morgaroth.quide.core.monitoring.CompState.StateAmplitude
import io.github.morgaroth.quide.core.register.QState._

/**
  * Created by morgaroth on 22.04.2016.
  */
object QStateDoesntWork {
  def props(startNo: Long, init: QValue = QValue.`0`) = Props(classOf[QStateDoesntWork], init, startNo)
}

class QStateDoesntWork(val init: QValue, val startNo: Long) extends QStateActor with Stash {

  def executing(gate: SingleQbitGate, myQbit: Char): Receive = {
    case MyAmplitude(ampl, _, no) if currentNo == no =>
      loginfo(s"received oppose amplitude $ampl from ${sender().path} task $no")
      amplitude = gate.execute(amplitude, ampl, myQbit)
      if (!ShallIDead()) {
        goAhead()
        unstashAll()
        context become receive
      } else {
        loginfo("I'm dying...")
      }
    case e =>
      stash()
      log.error(s"received $e during executiong stage, currento no == $currentNo")
  }

  override def receive: Receive = {
    case Execute(o@GateApply(operator: SingleQbitGate, targetBit), no) if currentNo == no =>
      loginfo(s"applying 1-qbit operator $operator.(no $no)")
      val (myQbit, opposedState) = findOpposedState(targetBit)
      unstashAll()
      context become executing(operator, myQbit)
      context.actorSelection(register / opposedState) ! MyAmplitude(amplitude, o, no)
    case Execute(o@GateApply(gate: ControlledGate, targetBit), no) if currentNo == no =>
      if (gate.controlBits.map(idx => myName.charAt(myName.length - idx - 1)).forall(_ == '1')) {
        loginfo(s"applying multi controlled operator $gate.(no $no)")
        val (myQbit, opposedState) = findOpposedState(targetBit)
        unstashAll()
        context become executing(gate.gate, myQbit)
        context.actorSelection(register / opposedState) ! MyAmplitude(amplitude, o, no)
      } else {
        loginfo("ignoring multi controlled gate, one of control bits is 0")
        goAhead()
      }
    case Execute(ReportValue(to), no) if currentNo == no =>
      loginfo(s"sending value to reporter.(no $no)")
      to ! StateAmplitude(myName, amplitude, no)
      goAhead()
    case Execute(_, no) if no > currentNo =>
      loginfo(s"ignoring $no")
      context.parent ! currentNo
    case Execute(_, no) if no < currentNo =>
      loginfo(s"ignoring DUE TO OLD $no")
    case m: MyAmplitude =>
      stash()
    case Ready =>
      context.parent ! currentNo
  }

  def goAhead(): Unit = {
    currentNo += 1
    context.parent ! currentNo
  }

}
