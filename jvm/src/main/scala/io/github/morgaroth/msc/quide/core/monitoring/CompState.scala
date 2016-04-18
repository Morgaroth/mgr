package io.github.morgaroth.msc.quide.core.monitoring

import akka.actor.{ActorRef, Props}
import io.github.morgaroth.msc.quide.core.actors.QuideActor
import io.github.morgaroth.msc.quide.core.monitoring.CompState.{Done, GetValue, StateAmplitude}
import io.github.morgaroth.msc.quide.model.QValue

import scala.collection.mutable
import scala.concurrent.duration._

/**
  * Created by mateusz on 04.01.16.
  */
object CompState {
  def props(size: Long) = Props(classOf[CompState], size)

  case class StateAmplitude(state: String, value: QValue, lastNo: Long)

  case object GetValue

  case object Done

}


class CompState(size: Long) extends QuideActor {

  import context.dispatcher

  val completed = mutable.Map.empty[String, QValue]
  var requesters = mutable.MutableList.empty[ActorRef]
  var collected = 0l
  val a = context.system.scheduler.scheduleOnce(2.seconds, self, Done)

  override def receive: Receive = {
    case GetValue =>
      log.info(s"registering ${sender()} for getting value")
      requesters += sender()

    case StateAmplitude(idx, value, _) =>
      log.info(s"getting value from qubit $idx")
      completed += idx -> value
      collected += 1
      if (collected == size) {
        requesters.foreach(_ ! completed.toMap)
        a.cancel()
        context stop self
      }

    case Done =>
      log.info("timer ends!, sending to requesters")
      requesters.foreach(_ ! completed.toMap)
      context stop self
  }
}
