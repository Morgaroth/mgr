package io.github.morgaroth.msc.quide.core.register

import akka.actor._
import io.github.morgaroth.msc.quide.core.actors.QuideActor
import io.github.morgaroth.msc.quide.core.register.QState.{Execute, GateApply, MyAmplitude}
import io.github.morgaroth.msc.quide.core.register.ZeroState.Creator

/**
  * Created by mateusz on 07.03.16.
  */
object ZeroState {
  type Creator = (Props, String) => ActorRef

  def props(registerName: ActorPath, creator: Creator): Props =
    Props(classOf[ZeroState], registerName, creator)
}

class ZeroState(registerName: ActorPath, actorCreator: Creator) extends QuideActor {
  override def receive: Receive = {
    case DeadLetter(MyAmplitude(ampl, gate, no), from, to) if from.path.parent == registerName =>
      log.info(s"received dead letter from $from (path=${from.path}) to $to (path=${to.path}")
      log.info(s"creating actor for name ${to.path.name}")
      val newStateActor = actorCreator(QState.props(), to.path.name)
      log.info(s"new actor path is ${newStateActor.path}")
      newStateActor ! Execute(gate, no)
      newStateActor ! MyAmplitude(ampl, gate, no)
    case d: DeadLetter =>
      log.warning(s"received illegal letter $d")
    case Execute(_, _) | GateApply(_, _) =>
    // ignore
  }
}
