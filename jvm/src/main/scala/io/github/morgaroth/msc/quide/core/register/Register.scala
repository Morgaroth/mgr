package io.github.morgaroth.msc.quide.core.register

import akka.actor.{ActorRef, Props}
import io.github.morgaroth.msc.quide.core.actors.QuideActor
import io.github.morgaroth.msc.quide.model.QbitValue
import io.github.morgaroth.msc.quide.model.QbitValue.`|0>`
import io.github.morgaroth.msc.quide.model.operators.Operator
import io.github.morgaroth.msc.quide.core.register.Qbit.{Execute, OperatorApply}
import io.github.morgaroth.msc.quide.core.register.Register.{ExecuteOperator, ReportValue}

/**
  * Created by mateusz on 03.01.16.
  */
object Register {
  //  def create(size: Int = 4, initVal: QbitValue = `|0>`)(implicit ac: ActorSystem) = {
  //    ac.actorOf(props(size, initVal))
  //  }

  def props(size: Int, initVal: QbitValue = `|0>`) = Props(classOf[Register], List.fill(size)(initVal))

  def props(initlVals: List[QbitValue]) = Props(classOf[Register], initlVals)

  //@formatter:off
  case class Step()
  case class ExecuteOperator(operator: Operator, qbit: Int)
  case class ReportValue(to: ActorRef)
  //@formatter:on
}


class Register(inits: List[QbitValue]) extends QuideActor {

  val qbits = inits.zipWithIndex map {
    case (value, idx) => context.actorOf(Qbit.props(idx, value), s"q$idx")
  }

  var no = 0l

  override def receive: Receive = {
    case ExecuteOperator(operator, onQubitNo) =>
      log.info(s"handling operator $operator")
      val receivers = qbits.slice(onQubitNo, onQubitNo + operator.size)
      val task = Execute(OperatorApply(operator), no)
      no += 1
      receivers.foreach(_ ! task)
    case ReportValue(to) =>
      qbits.foreach(_ ! Execute(Qbit.ReportValue(to), no))
      no += 1
  }
}