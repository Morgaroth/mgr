package io.github.morgaroth.msc.quide.core.register.sync

import akka.actor.{Props, Stash, Terminated}
import io.github.morgaroth.msc.quide.core.actors.QStateActor
import io.github.morgaroth.msc.quide.core.monitoring.CompState.{StateAmplitude, States}
import io.github.morgaroth.msc.quide.core.register.QState._
import io.github.morgaroth.msc.quide.model.QValue
import io.github.morgaroth.msc.quide.model.gates.{MultiControlledGate, SingleQbitGate}

/**
  * Created by mateusz on 07.03.16.
  */

object QStateSync {
  def props(startNo: Long, init: QValue = QValue.`0`) = Props(classOf[QStateSync], init, startNo)
}

class QStateSync(val init: QValue, val startNo: Long) extends QStateActor with Stash {

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
      startExecutionOf(o, operator, targetBit, no)
    case Execute(o@GateApply(gate: MultiControlledGate, targetBit), no) if currentNo == no =>
      if (gate.controlBits.map(idx => myName.charAt(myName.length - idx - 1)).forall(_ == '1')) {
        loginfo(s"applying multi controlled operator $gate.(no $no)")
        startExecutionOf(o, gate.gate, targetBit, no)
      } else {
        loginfo("ignoring multi controlled gate, one of control bits is 0")
        goAhead()
      }
    case Execute(ReportValue(to), no) if currentNo == no =>
      loginfo(s"sending value to reporter.(no $no)")
      to ! StateAmplitude(myName, amplitude, no)
      goAhead()
    case Execute(_, no) if no > currentNo =>
      loginfo(s"ignoring $no (current is $currentNo born with $startNo)")
      context.parent ! currentNo
    case Execute(_, no) if no < currentNo =>
      loginfo(s"ignoring DUE TO OLD $no")
    case m: MyAmplitude =>
      log.info(s"stashing $m")
      stash()
  }

  def startExecutionOf(o: GateApply, operator: SingleQbitGate, targetBit: Int, no: Long): Unit = {
    val (myQbit, opposedState) = findOpposedState(targetBit)
    unstashAll()
    context become executing(operator, myQbit)
    context.actorSelection(register / opposedState) ! MyAmplitude(amplitude, o, no)
  }

  def goAhead(): Unit = {
    parent ! Ready
    currentNo += 1
  }
}