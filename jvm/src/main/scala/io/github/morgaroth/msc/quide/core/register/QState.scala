package io.github.morgaroth.msc.quide.core.register

import javax.swing.SwingWorker.StateValue

import akka.actor.{ActorPath, ActorRef, Props}
import io.github.morgaroth.msc.quide.core.actors.QuideActor
import io.github.morgaroth.msc.quide.core.monitoring.CompState.StateAmplitude
import io.github.morgaroth.msc.quide.core.register.QState.{Execute, MyAmplitude, OperatorApply, ReportValue}
import io.github.morgaroth.msc.quide.model.QValue
import io.github.morgaroth.msc.quide.model.operators.{Operator, SingleQbitOperator}

/**
  * Created by mateusz on 07.03.16.
  */

object QState {

  //@formatter:off
  trait Action
  case class OperatorApply(operator: Operator, firstQbit:Int) extends Action
  case class ReportValue(to: ActorRef) extends Action
  case class Execute(action: Action, taskNo: Long)
  case class MyAmplitude(ampl: QValue)
  //@formatter:on


  def props(init: QValue = QValue.`0`) = Props(classOf[QState], init)
}

class QState(init: QValue) extends QuideActor {
  val register = self.path.parent
  val myName = self.path.name
  val deadAmplitude = QValue.`0`

  var amplitude: QValue = init
  var lastNo = -1l

  def ShallIDead() = {
    if (amplitude <= deadAmplitude) {
      context stop self
      true
    } else false
  }

  def executing(operator: SingleQbitOperator, myQbit: Char): Receive = {
    case MyAmplitude(ampl) =>
      log.info(s"received oppose amplitude $ampl from ${sender()}")
      amplitude = operator.execute(amplitude, ampl, myQbit)
      if (!ShallIDead()) {
        context become receive
      } else {
        log.info("I'm dying...")
      }
  }

  override def receive: Receive = {
    case Execute(OperatorApply(operator: SingleQbitOperator, index), no) =>
      log.info(s"applying 1-qbit operator $operator.(no $no)")
      lastNo = no
      val (myQbit, opposedState) = findOpposedState(index)
      context become executing(operator, myQbit)
      context.actorSelection(register / opposedState) ! MyAmplitude(amplitude)
    case Execute(ReportValue(to), no) =>
      log.info(s"sending value to reporter.(no $no)")
      to ! StateAmplitude(myName, amplitude, -1)
      lastNo = no

  }

  def findOpposedState(index: Int): (Char, String) = {
    val myQbit: Char = myName(myName.length - index - 1)
    val oponentQbit = if (myQbit == '0') '1' else '0'
    (myQbit, "%s%c%s".format(myName.slice(0, myName.length - index - 1), oponentQbit, myName.slice(myName.length - index, myName.length)))
  }
}
