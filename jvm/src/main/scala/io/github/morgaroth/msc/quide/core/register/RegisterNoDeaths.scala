package io.github.morgaroth.msc.quide.core.register

import akka.actor.Props
import io.github.morgaroth.msc.quide.core.actors.QuideActor
import io.github.morgaroth.msc.quide.core.register.QState.{Execute, GateApply, Ready}
import io.github.morgaroth.msc.quide.core.register.Register.{ExecuteGate, ReportValue}
import io.github.morgaroth.msc.quide.model.QValue

/**
  * Created by mateusz on 03.01.16.
  */
object RegisterNoDeaths {
  def props(size: Int): Props = props(InitState(List.fill(size)('0').mkString))

  def props(init: InitState): Props = Props(classOf[RegisterNoDeaths], init)
}

class RegisterNoDeaths(initState: InitState) extends QuideActor {

  if (initState.name.length > 25) {
    log.warning(s"too big state, possible OoM Error (current length is ${initState.name.length}")
  }

  log.info("")

  // experimental
  def createActors(prefix: String = "") {
    if (prefix.length < initState.name.length) {
      createActors(s"${prefix}0")
      createActors(s"${prefix}1")
    } else if (prefix.length == initState.name.length) {
      context.actorOf(QStateNoDeath.props(0, if (prefix.indexOf("1") < 0) initState.value else QValue.`0`), prefix)
    }
  }

  createActors()
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