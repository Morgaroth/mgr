package io.github.morgaroth.msc.quide.core.monitoring

import akka.actor.{ActorRef, Props}
import io.github.morgaroth.msc.quide.core.model.QbitValue
import io.github.morgaroth.msc.quide.core.monitoring.CompState.{Done, GetValue, QbitState}
import io.github.morgaroth.msc.quide.core.utilities.actors.QuideActor

import scala.collection.mutable

/**
  * Created by mateusz on 04.01.16.
  */
object CompState {
  def props(size: Int) = Props(classOf[CompState], size)

  case class QbitState(index: Int, value: QbitValue, lastNo: Long)

  case object GetValue

  case object Done

}


class CompState(size: Int) extends QuideActor {

  var waiting = Set.empty[Int] ++ (0 until size)
  val completed = mutable.Map.empty[Int, QbitValue]
  var requesters = mutable.MutableList.empty[ActorRef]

  override def receive: Receive = {
    case GetValue =>
      log.info(s"registering ${sender()} for getting value")
      requesters += sender()

    case QbitState(idx, value, _) =>
      log.info(s"getting value from qubit $idx")
      completed += idx -> value
      waiting -= idx
      if (waiting.isEmpty) {
        self ! Done
      }

    case Done =>
      log.info("all values completed, sending to requesters")
      requesters.foreach(_ ! completed.toMap)
      context stop self
  }
}
