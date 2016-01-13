package io.github.morgaroth.msc.quide.core.register

import akka.actor.{ActorRef, Props, Stash}
import akka.event.LoggingReceive
import io.github.morgaroth.msc.quide.core.actors.QuideActor
import io.github.morgaroth.msc.quide.core.monitoring.CompState.QbitState
import io.github.morgaroth.msc.quide.core.register.Qbit._
import io.github.morgaroth.msc.quide.model.QbitValue
import io.github.morgaroth.msc.quide.model.operators.Operator

import scala.collection.mutable

/**
  * Created by mateusz on 03.01.16.
  */
object Qbit {
  def props(index: Int, initValue: QbitValue) = Props(classOf[Qbit], index, initValue)

  //@formatter:off
  trait Action
  case class OperatorApply(operator: Operator, firstQbit:Int) extends Action
  case class ReportValue(to: ActorRef) extends Action
  case class Execute(action: Action, taskNo: Long)
  case class YourNeighbours(neighbours: List[ActorRef])
  case class HandleMyQbit(from: Int, value: QbitValue, no: Long)
  //@formatter:on
}

class Qbit(myIdx: Int, initValue: QbitValue) extends QuideActor with Stash {

  var lastNo = -1l
  var value = initValue

  var neighbours: NeighboursList = _
  val waiting = mutable.Map.empty[Int, mutable.Map[Int, QbitValue]]
  val waitingData = mutable.Map.empty[Int, Option[QbitValue]]

  override def receive = initializing

  def initializing = LoggingReceive {
    case YourNeighbours(n) =>
      neighbours = new NeighboursList(n, myIdx)
      context become working
      unstashAll()
      log.info("initialized with nghbrs list")
    case another => stash()
  }

  def waitingForData: Receive = LoggingReceive {
    case m@HandleMyQbit(from, qbit, no) =>
      log.info(s"received $m")
      waitingData += from -> Some(qbit)
      if (waitingData.forall(_._2.isDefined)) {
        // got all!

      }
  }

  def working: Receive = LoggingReceive {
    case Execute(OperatorApply(operator, _), no) if operator.size == 1 =>
      log.info(s"applying simple operator $operator.(no $no)")
      lastNo = no
      value = value.apply(operator)
    case Execute(OperatorApply(operator, firstIndex), no) =>
      log.info(s"applying complex operator $operator.(no $no)")
      // send qbit to neighbours in case of this operator
      val nbrsInThisOperator = neighbours.getInRangeOfOperator(operator, firstIndex)
      if (nbrsInThisOperator.nonEmpty) {
        // change state, wait for data
        context become waitingForData
        waitingData ++= nbrsInThisOperator.mapValues(_ => None)
        nbrsInThisOperator.foreach(_._2 ! HandleMyQbit(myIdx, value, no))
      } else {
        log.warning(s"WTF is here? multiqbit gate and no neighburs? DEBUG: o=$operator, myIdx=$myIdx, first=$firstIndex, no=$no")
      }
    case Execute(ReportValue(to), no) =>
      log.info(s"sending value to reporter.(no $no)")
      to ! QbitState(myIdx, value, lastNo)
      lastNo = no
  }
}


class NeighboursList(n: List[ActorRef], ownerIdx: Int) {
  def getInRangeOfOperator(o: Operator, firstIdx: Int): Map[Int, ActorRef] = {
    if (ownerIdx < firstIdx || ownerIdx > (o.size + firstIdx - 1)) {
      // WTF? owner not in operator range?
      throw new IllegalArgumentException("owner not in operator range?")
    } else {
      val all = n.zipWithIndex.slice(firstIdx, firstIdx + o.size).map(_.swap).toMap
      all - ownerIdx
    }
  }

}