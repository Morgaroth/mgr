package io.github.morgaroth.msc.quide.core.register

import akka.actor._
import io.github.morgaroth.msc.quide.core.actors.QuideActor
import io.github.morgaroth.msc.quide.core.register.QState.{OperatorApply, Execute}
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
    case DeadLetter(msg, from, to) if from.path.parent == registerName =>
      log.info(s"received dead letter from $from to $to")
    case d: DeadLetter =>
      log.warning(s"received illegal letter $d")
    case Execute | OperatorApply =>
    // ignore
  }
}
