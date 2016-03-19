package io.github.morgaroth.msc.quide.core.register

import akka.actor.{ActorRef, DeadLetter, Props}
import io.github.morgaroth.msc.quide.core.actors.QuideActor
import io.github.morgaroth.msc.quide.core.register.QState.{Execute, GateApply}
import io.github.morgaroth.msc.quide.core.register.Register.{ExecuteGate, ReportValue}
import io.github.morgaroth.msc.quide.model.QValue
import io.github.morgaroth.msc.quide.model.gates.Gate

/**
  * Created by mateusz on 03.01.16.
  */
object Register {
  def props(size: Int): Props = props(InitState(List.fill(size)('0').mkString))

  def props(init: InitState): Props = Props(classOf[Register], init)

  //@formatter:off
  case class Step()
  case class ExecuteGate(gate: Gate, qbit: Int)
  case class ReportValue(to: ActorRef)
  //@formatter:on
}

case class InitState(name: String, value: QValue = QValue.`1`) {
  val valid: Set[Char] = Set('1', '0')
  assert(name.forall(valid.contains))
}

class Register(initState: InitState) extends QuideActor {

  if (initState.name.length > 25) {
    log.warning(s"too big state, possible OoM Error (current length is ${initState.name.length}")
  }

  // create initial state actor
  context.actorOf(QState.props(initState.value), initState.name)

  // create zeroState mechanism
  val zeroState = context.actorOf(ZeroState.props(self.path, context.actorOf))
  context.system.eventStream.subscribe(zeroState, classOf[DeadLetter])

  var no = 0l

  override def receive: Receive = {
    case ExecuteGate(gate, targetBit) =>
      val task = Execute(GateApply(gate, targetBit), no)
      no += 1
      context.children.foreach(_ ! task)
    case ReportValue(to) =>
      context.children.foreach(_ ! Execute(QState.ReportValue(to), no))
      no += 1
  }
}