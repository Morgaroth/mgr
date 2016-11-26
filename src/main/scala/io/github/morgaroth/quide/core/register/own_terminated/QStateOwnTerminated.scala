package io.github.morgaroth.quide.core.register.own_terminated

import akka.actor.{Props, Stash}
import io.github.morgaroth.quide.core.actors.QStateActor
import io.github.morgaroth.quide.core.model.QValue
import io.github.morgaroth.quide.core.model.gates.{ControlledGate, SingleQbitGate}
import io.github.morgaroth.quide.core.monitoring.CompState.StateAmplitude
import io.github.morgaroth.quide.core.register.QState._
import io.github.morgaroth.quide.core.register.Register.ImDied

import scala.concurrent.duration._

/**
  * Created by mateusz on 07.03.16.
  */

object QStateOwnTerminated {
  def props(startNo: Long, init: QValue = QValue.`0`) = Props(classOf[QStateOwnTerminated], init, startNo)

}

class QStateOwnTerminated(val init: QValue, val startNo: Long) extends QStateActor with Stash {

  import context.dispatcher

  currentNo = startNo
  amplitude = init
  //  loginfo("Born!")
  context.system.scheduler.schedule(2.seconds, 5 seconds, self, INFO)

  def executing(gate: SingleQbitGate, myQbit: Char): Receive = {
    case MyAmplitude(ampl, _, no) if currentNo == no =>
//      loginfo(s"received oppose amplitude $ampl from ${sender().path} task $no")
      amplitude = gate.execute(amplitude, ampl, myQbit)
      if (!ShallIDead()) {
        goAhead()
        //        unstashAll()
        context become receive
      } else {
        //        loginfo(s"I'm dying... (no $currentNo)")
        context.stop(self)
        parent ! ImDied(self.path)
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

  override def receive: Receive = {
    case INFO =>
    //      log.error(s"actor $myName waiting state no $currentNo")
    case Execute(o@GateApply(operator: SingleQbitGate, targetBit), no) if currentNo == no =>
      //      loginfo(s"applying 1-qbit operator $operator.(no $no)")
      startExecutionOf(o, operator, targetBit, no)
    case Execute(o@GateApply(gate: ControlledGate, targetBit), no) if currentNo == no =>
      if (gate.controlBits.map(idx => myName.charAt(myName.length - idx - 1)).forall(_ == '1')) {
        //        loginfo(s"applying multi controlled operator $gate.(no $no)")
        startExecutionOf(o, gate.gate, targetBit, no)
      } else {
        // loginfo("ignoring multi controlled gate, one of control bits is 0")
        goAhead()
      }
    case Execute(ReportValue(to), no) if currentNo == no =>
      //      loginfo(s"sending value to reporter.(no $no)")
      to ! StateAmplitude(myName, amplitude, no)
      goAhead()
    case Execute(_, no) if no > currentNo =>
      //      loginfo(s"ignoring $no (current is $currentNo born with $startNo)")
      context.parent ! currentNo
    case Execute(_, no) if no < currentNo =>
      loginfo(s"ignoring DUE TO OLD $no")
    case m: MyAmplitude =>
      //      log.info(s"stashing $m")
      stash()
    case Ping =>
      parent ! Ready
    case z =>
      log.error(s"received illegal $z")
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