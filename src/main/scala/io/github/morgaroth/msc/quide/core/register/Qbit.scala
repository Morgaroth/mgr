package io.github.morgaroth.msc.quide.core.register

import akka.actor.{ActorRef, Props}
import io.github.morgaroth.msc.quide.core.model.{Operator, QbitValue}
import io.github.morgaroth.msc.quide.core.monitoring.CompState.QbitState
import io.github.morgaroth.msc.quide.core.register.Qbit.{Execute, OperatorApply, ReportValue}
import io.github.morgaroth.msc.quide.core.utilities.actors.QuideActor

/**
  * Created by mateusz on 03.01.16.
  */
object Qbit {
  def props(index: Int) = Props(classOf[Qbit], index)

  //@formatter:off
  trait Action
  case class OperatorApply(operator: Operator) extends Action
  case class ReportValue(to: ActorRef) extends Action
  case class Execute(action: Action, taskNo: Long)
  //@formatter:on
}

class Qbit(index: Int, initValue: QbitValue) extends QuideActor {

  var lastNo = -1l
  var value = initValue

  override def receive: Receive = {
    case Execute(OperatorApply(operator), no) if operator.size == 1 =>
      log.info(s"applying simple operator $operator.(no $no)")
      lastNo = no
      value = value.apply(operator)
    case Execute(OperatorApply(operator), no) =>
      log.info(s"applying complex operator $operator.(no $no)")
      log.warning("TO BE IMPLEMENTED")
    case Execute(ReportValue(to), no) =>
      log.info(s"sending value to reporter.(no $no)")
      to ! QbitState(index, value, lastNo)
      lastNo = no
  }
}
