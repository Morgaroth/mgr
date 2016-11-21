package io.github.morgaroth.quide.core.register.own_terminated

import akka.actor.{ActorPath, Cancellable, DeadLetter, Props, Stash, Terminated}
import io.github.morgaroth.quide.core.actors.QuideActor
import io.github.morgaroth.quide.core.monitoring.CompState.States
import io.github.morgaroth.quide.core.register.QState._
import io.github.morgaroth.quide.core.register.Register.{ExecuteGate, ImDied}
import io.github.morgaroth.quide.core.register.ZeroState.Creator
import io.github.morgaroth.quide.core.register.sync.RegisterSync.{CHECK, INFO}
import io.github.morgaroth.quide.core.register.{InitState, QState, ZeroState}

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by mateusz on 03.01.16.
  */
object RegisterOwnTerminated {
  def props(size: Int): Props = props(InitState(List.fill(size)('0').mkString))

  def props(init: InitState): Props = Props(classOf[RegisterOwnTerminated], init)

  case object INFO

  case object CHECK

}

class RegisterOwnTerminated(initState: InitState) extends QuideActor with Stash {

  import context.dispatcher

  if (initState.name.length > 25) {
    log.warning(s"too big state, possible OoM Error (current length is ${initState.name.length}")
  }

  var isReady = true
  var actors, waiting = collection.mutable.Set.empty[ActorPath]
  // create initial state actor
  val firstState = context.watch(context.actorOf(QStateOwnTerminated.props(0, initState.value), initState.name))
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
    context.watch(context.actorOf(QStateOwnTerminated.props(x, 0), name))
  }
  // create zeroState mechanism
  val zeroState = context.actorOf(ZeroState.props(self.path, creator), "zero")
  context.system.eventStream.subscribe(zeroState, classOf[DeadLetter])

  var no = 0l

  context.system.scheduler.schedule(2.seconds, 5 seconds, self, INFO)
  var call: Cancellable = _

  def scheduleCheck() {
    Option(call).foreach(_.cancel())
    call = context.system.scheduler.scheduleOnce(1.second, self, CHECK)
  }

  override def receive: Receive = {
    //    case any if {
    //      log.info(s"------------- any $any from ${sender()}")
    //      false
    //    } =>
    //
    case ExecuteGate(gate, targetBit) if isReady =>
      //      log.info(s"publishing task $gate on $targetBit")
      val task = Execute(GateApply(gate, targetBit), no)
      publishTask(task)
    case ExecuteGate(gate, targetBit) =>
      //      log.info(s"queuing gate $gate on $targetBit")
      stash()
    case ReportValue(to) if isReady =>
      //      log.info(s"publishing task RV")
      to ! States(context.children.size - 1)
      val task = Execute(QState.ReportValue(to), no)
      publishTask(task)
    case ReportValue(to) =>
      //      log.info("queuing task RV")
      stash()
    case Ready if isReady =>
      log.warning("Ready when ready?")
    case Terminated(_) if isReady =>
      log.warning("Terminated when ready?")
    case t: Terminated =>
    //      log.info(s"receiving terminated from ${t.actor.path.name} (no ${no - 1})")
    //      checkNext(t.actor.path)
    case ImDied(path) =>
      //      log.info(s"receiving terminated from ${path.name} (no ${no - 1})")
      checkNext(path)
    case Ready =>
      //      log.info(s"receiving ready from ${sender().path.name} (no ${no - 1})")
      actors += sender().path
      checkNext(sender().path)
    case INFO =>
    //      log.info(s"(no ${no - 1}) current ${context.children.size}, waiting ${waiting.map(_.name)}, actors = ${actors.map(_.name)}")
    case CHECK if isReady =>
    case CHECK =>
      //      log.info(s"CHECKING (no ${no - 1}) current ${context.children.size}, waiting ${waiting.map(_.name)}, actors = ${actors.map(_.name)}")
      if (waiting.size < actors.size) {
        waiting.foreach(context.actorSelection(_) ! Ping)
        //      } else {
        //        scheduleCheck()
      }
      scheduleCheck()
      checkNext(self.path)
    case z =>
      log.info(s"received $z")
  }

  def checkNext(without: ActorPath): Unit = {
    waiting -= without
    //    log.info(s"waiting.isEmpty ${waiting.isEmpty}, actors.size ${actors.size} == context.children.size ${context.children.size}")
    //    context.children.map(x => println(x.path))
    if (waiting.isEmpty && actors.size == context.children.size - 1) {
//      log.info("is ready! go ahead")
      isReady = true
      unstashAll()
    } else {
      scheduleCheck()
    }
  }

  def publishTask(task: Execute): Unit = {
    log.info(s"Task: ${task.taskNo}, task ${task.action}")
    context.children.foreach(_ ! task)
    no += 1
    isReady = false
    swapCollections()
    scheduleCheck()
  }
}