package io.github.morgaroth.quide.core.register.custom_map

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue, TimeUnit}

import akka.actor.{ActorRef, Cancellable, DeadLetter, Props, Stash, Terminated}
import io.github.morgaroth.quide.core.actors.QuideActor
import io.github.morgaroth.quide.core.monitoring.CompState.States
import io.github.morgaroth.quide.core.register.QState._
import io.github.morgaroth.quide.core.register.Register.{ExecuteGate, ImDiedOwn}
import io.github.morgaroth.quide.core.register.custom_map.QStateCustomMap.Initialize
import io.github.morgaroth.quide.core.register.custom_map.RegisterCustomMap.AppendWorking
import io.github.morgaroth.quide.core.register.sync.RegisterSync.{CHECK, INFO}
import io.github.morgaroth.quide.core.register.{InitState, QState, ZeroState}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

/**
  * Created by mateusz on 03.01.16.
  */
object RegisterCustomMap {
  def props(size: Int): Props = props(InitState(List.fill(size)('0').mkString))

  def props(init: InitState): Props = Props(classOf[RegisterCustomMap], init)

  case object INFO

  case object CHECK

  case class AppendWorking(x: (String, ActorRef))

}

class RegisterCustomMap(initState: InitState) extends QuideActor with Stash {

  import context.dispatcher

  val regSelf: ActorRef = self

  myName = "register"
  if (initState.name.length > 25) {
    log.warning(s"too big state, possible OoM Error (current length is ${initState.name.length}")
  }

  var no = 0l
  var isReady = true
  //  val waitingWorkers = collection.mutable.Queue.empty[ActorRef]
  val waitingWorkers = new LinkedBlockingQueue[ActorRef]()
  var readyStates, workingStates = collection.mutable.Map.empty[String, ActorRef]
  var states = Map.empty[String, ActorRef]

  def creator(x: Long, name: String) = {
    //    println(waitingWorkers.size, "when", name, x)
    val ref = Try(waitingWorkers.poll(0, TimeUnit.SECONDS)).filter(_ != null).getOrElse(context.actorOf(QStateCustomMap.props))
    regSelf ! AppendWorking(name -> ref)
//    workingStates += x
    scheduleWaitingActorsUpdate()
    ref ! Initialize(name, x)
    //    scheduleWaitingActorsUpdate()
    ref
  }

  // create zeroState mechanism
  lazy val zeroState = context.actorOf(ZeroState.props(self.path, creator), "zero")
  context.system.eventStream.subscribe(zeroState, classOf[DeadLetter])

  // create initial state actor
  val firstState = context.watch(context.actorOf(QStateCustomMap.props, initState.name))
  readyStates += (initState.name -> firstState)
  firstState ! Initialize(initState.name, no, initState.value)
  states = readyStates.toMap

  def swapCollections(): Unit = {
    val tmp = readyStates
    readyStates = workingStates
    workingStates = tmp
    if (readyStates.nonEmpty) {
      log.info("actors non empty!? WTF")
    }
  }

  val registerSize = initState.name.length.toLong
  val maxStatesCount = registerSize * registerSize
  val freeWorkersMax = registerSize * registerSize / 2

  def scheduleWaitingActorsUpdate() = {
        Future(ensureWaitingActorsSet())
//    ensureWaitingActorsSet()
  }

  def ensureWaitingActorsSet() {
    //    val usedActors = workingStates.size.toLong + readyStates.size
    //    val missingActorsToMax = maxStatesCount - usedActors
    //    val actorsShouldBeWaiting = math.min(freeWorkersMax, missingActorsToMax)
    //
    //    // waitingWorkers < actorsShouldBeWaiting, born missing
    //    waitingWorkers.size.toLong until actorsShouldBeWaiting foreach { _ =>
    //      waitingWorkers += context.actorOf(QStateCustomMap.props(0))
    //    }
    //    // waitingWorkers > actorsShouldBeWaiting, kill some of them
    //    print(actorsShouldBeWaiting, waitingWorkers.size)
    //    actorsShouldBeWaiting until waitingWorkers.size.toLong foreach { _ =>
    //      context stop waitingWorkers.dequeue()
    //    }
    (waitingWorkers.size.toLong until freeWorkersMax) foreach { _ =>
      waitingWorkers.put(context.actorOf(QStateCustomMap.props))
    }
  }

  ensureWaitingActorsSet()

  context.system.scheduler.schedule(2.seconds, 5 seconds, self, INFO)
  var call: Cancellable = _

  def scheduleCheck() {
    Option(call).foreach(_.cancel())
    call = context.system.scheduler.scheduleOnce(5.seconds, self, CHECK)
  }

  override def receive: Receive = {
    //    case any if {
    //      log.info(s"------------- any $any from ${sender()}")
    //      false
    //    } =>
    case ExecuteGate(gate, targetBit) if isReady =>
      //      log.info(s"publishing task $gate on $targetBit")
      publishTask(GateApply(gate, targetBit))
    case ExecuteGate(gate, targetBit) =>
      //      log.info(s"queuing gate $gate on $targetBit")
      stash()
    case ReportValue(to) if isReady =>
      //      log.info(s"publishing task RV")
      to ! States(readyStates.size)
      publishTask(QState.ReportValue(to))
    case ReportValue(to) =>
      //      log.info("queuing task RV")
      stash()
    case ReadyOwn if isReady =>
      log.warning("Ready when ready?")
    //    case Terminated(_) if isReady =>
    //      log.warning("Terminated when ready?")
    case t: Terminated =>
    //      log.info(s"receiving terminated from ${t.actor.path.name} (no ${no - 1})")
    //      checkNext(t.actor.path)
    case ImDiedOwn(state, worker) =>
      //      log.info(s"receiving terminated from ${path.name} (no ${no - 1})")

      checkNext(state)
      waitingWorkers.put(sender())
      scheduleWaitingActorsUpdate()
    case ReadyOwn(name) =>
      //      log.info(s"receiving ready from $name (no ${no - 1})")
      readyStates += (name -> sender())
      checkNext(name)
    case AppendWorking(x) =>
      //      loginfo(s"appending $x")
      workingStates += x
//      ensureWaitingActorsSet()
    case INFO =>
    //      log.info(s"(no ${no - 1}) current ${context.children.size}, waiting ${waiting.map(_.name)}, actors = ${actors.map(_.name)}")
    case CHECK if isReady =>
      loginfo("WTF")
    case CHECK =>
      //      loginfo(s"CHECKING (no ${no - 1}), workingStates = ${workingStates.keys}, readyStates ${readyStates.keys}")
      if (workingStates.size < readyStates.size) {
        workingStates.values.foreach(_ ! Ping)
        //      } else {
        //        scheduleCheck()
      }
      scheduleCheck()
      checkNext("")
    case z =>
      loginfo(s"received $z")
  }

  def checkNext(state: String): Unit = {
    workingStates.remove(state)
    //    log.info(s"marking $state, workingStates = ${workingStates.keys}, readyStates ${readyStates.keys}")
    //    context.children.map(x => println(x.path))
    if (workingStates.isEmpty) {
      //      log.info("is ready! go ahead")
      states = readyStates.toMap
      isReady = true
      unstashAll()
    } else {
      scheduleCheck()
    }
  }

  def publishTask(action: QState.Action): Unit = {
    log.info(s"Task: $no, task $action")
    val effectiveTask = ExecuteOwn(action, no, states)
    readyStates.values.foreach(_ ! effectiveTask)
    no += 1
    isReady = false
    swapCollections()
    scheduleCheck()
  }
}