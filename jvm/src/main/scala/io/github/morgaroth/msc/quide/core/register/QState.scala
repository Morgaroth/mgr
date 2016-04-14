package io.github.morgaroth.msc.quide.core.register

import akka.actor.{ActorRef, Props, Stash}
import io.github.morgaroth.msc.quide.core.actors.QuideActor
import io.github.morgaroth.msc.quide.core.monitoring.CompState.StateAmplitude
import io.github.morgaroth.msc.quide.core.register.QState.{Execute, GateApply, MyAmplitude, ReportValue}
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
  case object NoOp extends Action
  case class MyAmplitude(ampl: QValue, op: GateApply, taskNo: Long)
  //@formatter:on


  def props(startNo: Long, init: QValue = QValue.`0`) = Props(classOf[QState], init, startNo)
}

class QState(init: QValue, startNo: Long) extends QuideActor with Stash {
  val register = self.path.parent
  val myName = self.path.name
  val deadAmplitude = QValue.`0`

  var amplitude: QValue = init
  var currentNo = startNo

  val history = context.actorSelection("../history")

  def loginfo(msg: String) = {
    log.info(s"|$myName> - $msg")
  }

  def ShallIDead() = {
    if (amplitude <= deadAmplitude) {
      context stop self
      true
    } else false
  }

  def executing(gate: SingleQbitGate, myQbit: Char): Receive = {
    case MyAmplitude(ampl, _, no) if currentNo == no =>
      loginfo(s"received oppose amplitude $ampl from ${sender().path} task $no")
      amplitude = gate.execute(amplitude, ampl, myQbit)
      if (!ShallIDead()) {
        currentNo += 1
        unstashAll()
        context become receive
      } else {
        loginfo("I'm dying...")
      }
    case _ =>
      stash()
  }

  override def receive: Receive = {
    case Execute(o@GateApply(operator: SingleQbitGate, targetBit), no) if currentNo == no =>
      loginfo(s"applying 1-qbit operator $operator.(no $no)")
      val (myQbit, opposedState) = findOpposedState(targetBit)
      context become executing(operator, myQbit)
      context.actorSelection(register / opposedState) ! MyAmplitude(amplitude, o, no)
    case Execute(o@GateApply(gate: MultiControlledGate, targetBit), no) if currentNo == no =>
      if (gate.controlBits.map(idx => myName.charAt(myName.length - idx - 1)).forall(_ == '1')) {
        loginfo(s"applying multi controlled operator $gate.(no $no)")
        val (myQbit, opposedState) = findOpposedState(targetBit)
        context become executing(gate.gate, myQbit)
        context.actorSelection(register / opposedState) ! MyAmplitude(amplitude, o, no)
      } else {
        loginfo("ignoring multi controlled gate, one of control bits is 0")
        currentNo += 1
      }
    case Execute(ReportValue(to), no) if currentNo == no =>
      loginfo(s"sending value to reporter.(no $no)")
      to ! StateAmplitude(myName, amplitude, no)
      currentNo += 1
    case Execute(_, no) if no > currentNo =>
      loginfo(s"stashing $no")
      history ! currentNo
      stash()

  }

  def findOpposedState(index: Int): (Char, String) = {
    val myQbit: Char = myName(myName.length - index - 1)
    val oponentQbit = if (myQbit == '0') '1' else '0'
    (myQbit, "%s%c%s".format(myName.slice(0, myName.length - index - 1), oponentQbit, myName.slice(myName.length - index, myName.length)))
  }
}
