package io.github.morgaroth.msc.quide.core.register.doesntwork

import akka.actor.{DeadLetter, Props}
import io.github.morgaroth.msc.quide.core.actors.QuideActor
import io.github.morgaroth.msc.quide.core.register.QState.{Execute, GateApply, Ready}
import io.github.morgaroth.msc.quide.core.register.Register.{ExecuteGate, ReportValue}
import io.github.morgaroth.msc.quide.core.register.{History, InitState, QState, ZeroState}

/**
  * Created by morgaroth on 22.04.2016.
  */
object RegisterDoesntWork {
  def props(size: Int): Props = props(InitState(List.fill(size)('0').mkString))

  def props(init: InitState): Props = Props(classOf[RegisterDoesntWork], init)

}


class RegisterDoesntWork(initState: InitState) extends QuideActor {

  if (initState.name.length > 25) {
    log.warning(s"too big state, possible OoM Error (current length is ${initState.name.length}")
  }

  // create initial state actor
  context.actorOf(QStateDoesntWork.props(0, initState.value), initState.name)
  // create zeroState mechanism
  val zeroState = context.actorOf(ZeroState.props(self.path, (x: Long, name: String) => context.actorOf(Props(classOf[QStateDoesntWork], x), name)), "zero")
  val history = context.actorOf(Props[History], "history")
  context.system.eventStream.subscribe(zeroState, classOf[DeadLetter])

  var no = 0l

  val tasks = collection.mutable.Map.empty[Long, QState.Execute]

  override def receive: Receive = {
    case ExecuteGate(gate, targetBit) =>
      val task = Execute(GateApply(gate, targetBit), no)
      tasks += no -> task
      context.children.foreach(_ ! Ready)
      no += 1
    case ReportValue(to) =>
      val task = Execute(QState.ReportValue(to), no)
      tasks += no -> task
      context.children.foreach(_ ! Ready)
      no += 1
    case taskNo: Long =>
      tasks.get(taskNo).foreach(sender() ! _)
  }
}
