package io.github.morgaroth.quide.core.register.sync

import akka.actor.{ActorPath, DeadLetter, Props, Stash, Terminated}
import io.github.morgaroth.quide.core.actors.QuideActor
import io.github.morgaroth.quide.core.monitoring.CompState.States
import io.github.morgaroth.quide.core.register.QState.{Execute, GateApply, Ready, ReportValue}
import io.github.morgaroth.quide.core.register.Register.ExecuteGate
import io.github.morgaroth.quide.core.register.{InitState, QState, ZeroState}
import io.github.morgaroth.quide.core.register.ZeroState.Creator

import scala.collection.mutable

/**
  * Created by mateusz on 03.01.16.
  */
object RegisterSync {
  def props(size: Int): Props = props(InitState(List.fill(size)('0').mkString))

  def props(init: InitState): Props = Props(classOf[RegisterSync], init)
}

class RegisterSync(initState: InitState) extends QuideActor with Stash {

  if (initState.name.length > 25) {
    log.warning(s"too big state, possible OoM Error (current length is ${initState.name.length}")
  }

  var isReady = true
  var actors, waiting = collection.mutable.Set.empty[ActorPath]
  var tmp: mutable.Set[ActorPath] = _
  // create initial state actor
  val firstState = context.watch(context.actorOf(QStateSync.props(0, initState.value), initState.name))
  actors += firstState.path

  def swapCollections(): Unit = {
    tmp = actors
    actors = waiting
    waiting = tmp
    if (actors.nonEmpty) {
      log.info("actors non empty!? WTF")
    }
  }

  private val creator: Creator = (x: Long, name: String) => {
    context.watch(context.actorOf(QStateSync.props(x, 0), name))
  }
  // create zeroState mechanism
  val zeroState = context.actorOf(ZeroState.props(self.path, creator), "zero")
  context.system.eventStream.subscribe(zeroState, classOf[DeadLetter])

  var no = 0l

  override def receive: Receive = {
    case ExecuteGate(gate, targetBit) if isReady =>
      log.info(s"publishing task $gate on $targetBit")
      val task = Execute(GateApply(gate, targetBit), no)
      publishTask(task)
    case ExecuteGate(gate, targetBit) =>
      log.info(s"queuing gate $gate on $targetBit")
      stash()
    case ReportValue(to) if isReady =>
      log.info(s"publishing task RV")
      to ! States(context.children.size - 1)
      val task = Execute(QState.ReportValue(to), no)
      publishTask(task)
    case ReportValue(to) =>
      log.info("queuing task RV")
      stash()
    case Ready if isReady =>
      log.warning("Ready when ready?")
    case Terminated(_) if isReady =>
      log.warning("Terminated when ready?")
    case t: Terminated =>
      log.info(s"receiving terminated from ${t.actor.path.name}")
      checkNext(t.actor.path)
    case Ready =>
      log.info(s"receiving ready from ${sender().path.name}")
      actors += sender().path
      checkNext(sender().path)
    case z =>
      log.info(s"received $z")
  }

  def checkNext(without: ActorPath): Unit = {
    waiting -= without
    log.info(s"checking next with waiting $waiting")
    if (waiting.isEmpty && actors.size == context.children.size - 1) {
      log.info("is ready! go ahead")
      isReady = true
      unstashAll()
    }
  }

  def publishTask(task: Execute): Unit = {
    context.children.foreach(_ ! task)
    no += 1
    isReady = false
    swapCollections()
  }
}