package io.github.morgaroth.quide.core.monitoring

import akka.actor.{ActorRef, Props}
import io.github.morgaroth.quide.core.actors.QuideActor
import io.github.morgaroth.quide.core.model.QValue
import io.github.morgaroth.quide.core.monitoring.CompState.{Done, GetValue, StateAmplitude, States}

import scala.collection.mutable
import scala.concurrent.duration._

/**
  * Created by mateusz on 04.01.16.
  */
object CompState {
  def props(size: Long) = Props(classOf[CompState], size)

  case class StateAmplitude(state: String, value: QValue, lastNo: Long)

  case class States(alive: Long)

  case object GetValue

  case object Done

}


class CompState(size: Long) extends QuideActor {

  import context.dispatcher

  val completed = mutable.Map.empty[String, QValue]
  var requesters = mutable.MutableList.empty[ActorRef]
  var collected = 0l
  var targetValues = size
  val a = context.system.scheduler.scheduleOnce(2.seconds, self, Done)

  override def receive: Receive = {
    case GetValue =>
      log.info(s"registering ${sender()} for getting value")
      requesters += sender()
    case States(howMuch) =>
      targetValues = howMuch
    case StateAmplitude(idx, value, _) =>
      log.info(s"getting value from qubit $idx")
      if (value >= 0.001d) {
        completed += idx -> value
      }
      collected += 1
      if (collected == targetValues) {
        a.cancel()
        finish
      }

    case Done =>
      log.info("timer ends!, sending to requesters")
      finish
  }

  def finish: Unit = {
    requesters.foreach(_ ! completed.toMap)
    context stop self
  }
}
