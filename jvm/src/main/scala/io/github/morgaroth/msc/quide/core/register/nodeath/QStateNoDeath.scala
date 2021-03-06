package io.github.morgaroth.msc.quide.core.register.nodeath

import akka.actor.{Cancellable, Props, Stash}
import io.github.morgaroth.msc.quide.core.actors.{QStateActor, QuideActor}
import io.github.morgaroth.msc.quide.core.monitoring.CompState.StateAmplitude
import io.github.morgaroth.msc.quide.core.register.QState._
import io.github.morgaroth.msc.quide.model.QValue
import io.github.morgaroth.msc.quide.model.gates.{MultiControlledGate, SingleQbitGate}

import scala.concurrent.duration._

/**
  * Created by mateusz on 07.03.16.
  */

object QStateNoDeath {
  def props(startNo: Long, init: QValue = QValue.`0`) = Props(classOf[QStateNoDeath], init, startNo)
}

class QStateNoDeath(val init: QValue, val startNo: Long) extends QStateActor with Stash {

  val deathTime = 50.millis
  var deathTimer: Option[Cancellable] = None

  loginfo("Born!")

  def executing(gate: SingleQbitGate, myQbit: Char): Receive = {
    case MyAmplitude(ampl, _, no) if currentNo == no =>
      amplitude = gate.execute(amplitude, ampl, myQbit)
      goAhead()
      unstashAll()
      context become receive
    case e =>
      stash()
    //      log.error(s"|$myName> - received $e during executiong stage, currento no == $currentNo")
  }

  override def receive: Receive = {
    case Execute(o@GateApply(operator: SingleQbitGate, targetBit), no) if currentNo == no =>
      //      loginfo(s"applying 1-qbit operator $operator.(no $no)")
      val (myQbit, opposedState) = findOpposedState(targetBit)
      unstashAll()
      context become executing(operator, myQbit)
      context.actorSelection(register / opposedState) ! MyAmplitude(amplitude, o, no)
    case Execute(o@GateApply(gate: MultiControlledGate, targetBit), no) if currentNo == no =>
      if (gate.controlBits.map(idx => myName.charAt(myName.length - idx - 1)).forall(_ == '1')) {
        //        loginfo(s"applying multi controlled operator $gate.(no $no)")
        val (myQbit, opposedState) = findOpposedState(targetBit)
        unstashAll()
        context become executing(gate.gate, myQbit)
        context.actorSelection(register / opposedState) ! MyAmplitude(amplitude, o, no)
      } else {
        //        loginfo("ignoring multi controlled gate, one of control bits is 0")
        goAhead()
      }
    case Execute(ReportValue(to), no) if currentNo == no =>
      //      loginfo(s"sending value to reporter.(no $no)")
      to ! StateAmplitude(myName, amplitude, no)
      goAhead()
    case Execute(_, no) if no > currentNo =>
      //      loginfo(s"ignoring $no")
      context.parent ! currentNo
    case Execute(_, no) if no < currentNo =>
      //      loginfo(s"ignoring DUE TO OLD $no (current $currentNo)")
      context.parent ! currentNo
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