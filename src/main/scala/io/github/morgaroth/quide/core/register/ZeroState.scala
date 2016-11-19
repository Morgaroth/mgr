package io.github.morgaroth.quide.core.register

import akka.actor._
import io.github.morgaroth.quide.core.actors.QuideActor
import io.github.morgaroth.quide.core.register.QState._
import io.github.morgaroth.quide.core.register.ZeroState.Creator

/** w
  * Created by mateusz on 07.03.16.
  */
object ZeroState {
  type Creator = (Long, String) => ActorRef

  def props(registerName: ActorPath, creator: Creator): Props =
    Props(classOf[ZeroState], registerName, creator)
}

class ZeroState(registerName: ActorPath, actorCreator: Creator) extends QuideActor {
  var currentNo = 0

  override def receive: Receive = {
    case DeadLetter(MyAmplitude(ampl, gate, no), from, to) if from.path.parent == registerName =>
//      log.info(s"received amplitude letter from $from (path=${from.path}) to $to (path=${to.path} no is $no")
      //      log.info(s"creating actor for name ${to.path.name}")
      val newStateActor = actorCreator(no, to.path.name)
      //      log.info(s"new actor path is ${newStateActor.path}")
      newStateActor.tell(Execute(gate, no), from)
      newStateActor.tell(MyAmplitude(ampl, gate, no), from)
    case DeadLetter(INFO, _, _) =>
    case DeadLetter(Ping, from, to) =>
      log.info(s"handling Ping from $from to $to")
      context.parent.!(Terminated(to)(existenceConfirmed = true, addressTerminated = true))(to)
    case DeadLetter(data, from, to) =>
      log.warning(s"received illegal letter DeadLetter($data,${from.path},${to.path})")
    case Execute(_, _) | GateApply(_, _) | Ready =>
    // ignore
  }
}
