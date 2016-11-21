package io.github.morgaroth.quide.core.register.custom_map

import akka.actor.{ActorRef, Props, Stash}
import io.github.morgaroth.quide.core.actors.QStateBase
import io.github.morgaroth.quide.core.model.QValue
import io.github.morgaroth.quide.core.model.gates.{ControlledGate, SingleQbitGate}
import io.github.morgaroth.quide.core.monitoring.CompState.StateAmplitude
import io.github.morgaroth.quide.core.register.QState.{MyAmplitude => _, _}
import io.github.morgaroth.quide.core.register.Register.{ImDied, ImDiedOwn}
import io.github.morgaroth.quide.core.register.custom_map.QStateCustomMap.Initialize

import scala.concurrent.duration._

/**
  * Created by mateusz on 07.03.16.
  */

object QStateCustomMap {
  def props = Props(classOf[QStateCustomMap])

  case class Initialize(stateName: String, currentNo: Long, initAmplitude: QValue = QValue.`0`)

}

class QStateCustomMap extends QStateBase with Stash {

  import context.dispatcher

  //  loginfo("Born!")
  context.system.scheduler.schedule(2.seconds, 5 seconds, self, INFO)

  override def receive: Receive = waiting

  def waiting: Receive = {
    case Initialize(name, no, amp) =>
      myName = name
      //      loginfo(s"initialized with no $no and amp $amp")
      amplitude = amp
      currentNo = no
      context become doWork
    case INFO =>
    case z =>
      loginfo(s"received $z in waiting state")
  }

  def executing(gate: SingleQbitGate, myQbit: Char): Receive = {
    case MyAmplitudeOwn(ampl, _, no, _, _) if currentNo == no =>
      //      loginfo(s"received oppose amplitude $ampl from ${sender().path} task $no")
      amplitude = gate.execute(amplitude, ampl, myQbit)
      if (!ShallIDead()) {
        goAhead()
        //        unstashAll()
        context become doWork
      } else {
        loginfo(s"I'm dying... (no $currentNo)")
        parent ! ImDiedOwn(myName, self)
        context become waiting
      }
    case Ping =>
      //      loginfo(s"got ping during executing $gate (no $currentNo)")
      parent ! Busy
    case INFO =>
    //      log.error(s"actor $myName in state executing with no $currentNo in $gate")
    case e =>
      //      stash()
      log.error(s"received $e during executiong stage, currento no == $currentNo")
  }

  def doWork: Receive = {
    case INFO =>
    //      log.error(s"actor $myName waiting state no $currentNo")
    case ExecuteOwn(o@GateApply(operator: SingleQbitGate, targetBit), no, states) if currentNo == no =>
      //      loginfo(s"applying 1-qbit operator $operator.(no $no)")
      startExecutionOf(o, operator, targetBit, no, states)
    case ExecuteOwn(o@GateApply(gate: ControlledGate, targetBit), no, states) if currentNo == no =>
      if (gate.controlBits.map(idx => myName.charAt(myName.length - idx - 1)).forall(_ == '1')) {
        //        loginfo(s"applying multi controlled operator $gate.(no $no)")
        startExecutionOf(o, gate.gate, targetBit, no, states)
      } else {
        // loginfo("ignoring multi controlled gate, one of control bits is 0")
        goAhead()
      }
    case ExecuteOwn(ReportValue(to), no, _) if currentNo == no =>
      //            loginfo(s"sending value to reporter.(no $no)")
      to ! StateAmplitude(myName, amplitude, no)
      goAhead()
    case Execute(_, no) if no < currentNo =>
      loginfo(s"ignoring DUE TO OLD $no")
    case m: MyAmplitudeOwn =>
      //      loginfo(s"stashing $m")
      stash()
    case Ping =>
      parent ! Ready
    case z =>
      log.error(s"received illegal dowork no ($currentNo) $z")
  }

  def startExecutionOf(o: GateApply, operator: SingleQbitGate, targetBit: Int, no: Long, states: Map[String, ActorRef]): Unit = {
    val (myQbit, opposedState) = findOpposedState(targetBit)
    unstashAll()
    context become executing(operator, myQbit)
    states.getOrElse(opposedState, context.system.deadLetters) ! MyAmplitudeOwn(amplitude, o, no, opposedState, states)
  }

  def goAhead(): Unit = {
    parent ! ReadyOwn(myName)
    currentNo += 1
  }
}