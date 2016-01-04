package io.github.morgaroth.msc.quide.core.register

import akka.actor.{ActorRef, ActorSystem, Props}
import io.github.morgaroth.msc.quide.core.model.Operator
import io.github.morgaroth.msc.quide.core.register.Qbit.{OperatorApply, Execute}
import io.github.morgaroth.msc.quide.core.register.Register.{ReportValue, ExecuteOperator}
import io.github.morgaroth.msc.quide.core.utilities.actors.QuideActor

/**
  * Created by mateusz on 03.01.16.
  */
object Register {
  def create(size: Int = 4)(implicit ac: ActorSystem) = {
    ac.actorOf(props(size))
  }

  def props(size: Int) = Props(classOf[Register], size)

  //@formatter:off
  case class Step()
  case class ExecuteOperator(operator: Operator, qbit: Int)
  case class ReportValue(to: ActorRef)
  //@formatter:on
}


class Register(size: Int) extends QuideActor {

  val qbits = 0 until size map { idx =>
    context.actorOf(Qbit.props(idx), s"q$idx")
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