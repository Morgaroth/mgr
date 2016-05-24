package io.github.morgaroth.quide.core.register.exc

import akka.actor.SupervisorStrategy.{Escalate, Stop}
import akka.actor.{ActorPath, DeadLetter, OneForOneStrategy, PoisonPill, Props, Stash, SupervisorStrategy, Terminated}
import akka.dispatch.sysmsg.Terminate
import io.github.morgaroth.quide.core.actors.QuideActor
import io.github.morgaroth.quide.core.monitoring.CompState.States
import io.github.morgaroth.quide.core.register.QState.{Execute, GateApply, Ready, ReportValue}
import io.github.morgaroth.quide.core.register.Register.ExecuteGate
import io.github.morgaroth.quide.core.register.ZeroState.Creator
import io.github.morgaroth.quide.core.register.exc.QStateExc.ForcedTermination
import io.github.morgaroth.quide.core.register.exc.RegisterExc.MyPoison
import io.github.morgaroth.quide.core.register.sync.RegisterSync.INFO
import io.github.morgaroth.quide.core.register.{InitState, QState, ZeroState}

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by mateusz on 03.01.16.
  */
object RegisterExc {
  def props(size: Int): Props = props(InitState(List.fill(size)('0').mkString))

  def props(init: InitState): Props = Props(classOf[RegisterExc], init)

  case object INFO

  case object MyPoison

}

class RegisterExc(initState: InitState) extends QuideActor with Stash {

  import context.dispatcher

  if (initState.name.length > 25) {
    log.warning(s"too big state, possible OoM Error (current length is ${initState.name.length}")
  }

  var isReady = true
  var actors, waiting = collection.mutable.Set.empty[ActorPath]
  val exceptioned = collection.mutable.Set.empty[(ActorPath, Long)]
  // create initial state actor
  val firstState = context.watch(context.actorOf(QStateExc.props(0, initState.value), initState.name))
  actors += firstState.path

  def swapCollections(): Unit = {
    val tmp = actors
    actors = waiting
    waiting = tmp
    if (actors.nonEmpty) {
      log.info("actors non empty!? WTF")
    }
  }

  private val creator: Creator = (x: Long, name: String) => {
    context.watch(context.actorOf(QStateExc.props(x, 0), name))
  }
  // create zeroState mechanism
  val zeroState = context.actorOf(ZeroState.props(self.path, creator), "zero")
  context.system.eventStream.subscribe(zeroState, classOf[DeadLetter])

  var no = 0l

  context.system.scheduler.schedule(2.seconds, 10 seconds, self, INFO)

  override def receive: Receive = {
    case ExecuteGate(gate, targetBit) if isReady =>
      val task = Execute(GateApply(gate, targetBit), no)
      publishTask(task)
    case ExecuteGate(gate, targetBit) =>
      stash()
    case ReportValue(to) if isReady =>
      to ! States(context.children.size - 1)
      val task = Execute(QState.ReportValue(to), no)
      publishTask(task)
    case ReportValue(to) =>
      stash()
    case Terminated(_) if isReady =>
    //      log.warning(s"Terminated when ready? (no $no)")
    case t: Terminated =>
      log.info(s"Receiving terminated from ${t.actor.path.name} (no ${no - 1})")
      checkNext(t.actor.path, no - 1)
    case Ready =>
      log.info(s"Receiving ready from ${sender().path.name} (no ${no - 1})")
      actors += sender().path
      checkNext(sender().path, no - 1)
    case INFO =>
      log.error(s"(no ${no - 1}) current ${context.children.size}, waiting ${waiting.map(_.name)}, actors = ${actors.map(_.name)}, exceptioned ${exceptioned.map(x => x.copy(_1 = x._1.name))}")
    case z =>
      log.error(s"received $z")
  }

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = -1, withinTimeRange = Duration.Inf, loggingEnabled = false) {
      case ForcedTermination(name, number) if number == no - 1 =>
        remember(name, number)
        Stop
      case ForcedTermination(name, number) =>
        log.warning(s"termination of $name during task $number received on task ${no - 1}")
        Stop
      case _: Exception => Escalate
    }

  //  var lastlog = "qwertyui"

  def checkNext(without: ActorPath, no: Long): Unit = {
    waiting -= without
    exceptioned -= without -> no
    //    lastlog = s"checking end with ${actors.map(_.name)} without ${without.name} and ready ${actors.map(_.name)} no ${no - 1}"
    if (waiting.isEmpty && actors.size == context.children.size - 1) {
      log.info("is ready! go ahead")
      isReady = true
      unstashAll()
    }
  }


  def remember(without: ActorPath, no: Long): Unit = {
    exceptioned += without -> no
  }

  def publishTask(task: Execute): Unit = {
    log.warning(s"publishing $task")
    context.children.foreach(_ ! task)
    no += 1
    isReady = false
    swapCollections()
  }
}