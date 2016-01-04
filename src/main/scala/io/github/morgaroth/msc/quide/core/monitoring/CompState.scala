package io.github.morgaroth.msc.quide.core.monitoring

import akka.actor.Props
import io.github.morgaroth.msc.quide.core.model.QbitValue
import io.github.morgaroth.msc.quide.core.monitoring.CompState.QbitState
import io.github.morgaroth.msc.quide.core.utilities.actors.QuideActor

/**
  * Created by mateusz on 04.01.16.
  */
object CompState {
  def props(size: Int) = Props(classOf[CompState], size)

  case class QbitState(index: Int, value: QbitValue, lastNo: Long)

}


class CompState(size: Int) extends QuideActor {

  val waiting = Set.empty[Int] ++ (0 until size)


  override def receive: Receive = {
    case QbitState(idx, value, _) =>
  }
}
