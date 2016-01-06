package io.github.morgaroth.msc.quide.core.register

import akka.actor.{ActorRef, ActorSystem}
import io.github.morgaroth.msc.quide.core.BaseQuideActorTest
import io.github.morgaroth.msc.quide.core.model.QbitValue._
import io.github.morgaroth.msc.quide.core.model.{QbitValue, QbitValues, H, X}
import io.github.morgaroth.msc.quide.core.monitoring.CompState.QbitState
import io.github.morgaroth.msc.quide.core.register.Register.{ExecuteOperator, ReportValue}

import scala.language.postfixOps

/**
  * Created by mateusz on 06.01.16.
  */
class RegisterTest(as: ActorSystem) extends BaseQuideActorTest(as) {

  def this() = this(ActorSystem("QbitTest"))

  def withActor(size: Int = 5)(code: ActorRef => Unit) = {
    code(system.actorOf(Register.props(5, `|0>`)))
  }

  "Register actor" must {
    "create correct qbit actors during initialization" in withActor() { actor =>
      actor ! ReportValue(testActor)
      val indexes = 0 until 5 map (_ => expectMsgClass(classOf[QbitState])) map (_.index)
      indexes.sorted should equal(0 until 5)
    }

    "execute correctly gates" in withActor(2) { actor =>
      actor ! ExecuteOperator(H, 0)
      actor ! ExecuteOperator(X, 1)
      actor ! ReportValue(testActor)
      val indexes: Map[Int, QbitValue] = 0 until 5 map (_ => expectMsgClass(classOf[QbitState])) map (x => x.index -> x.value) toMap

      indexes(0) should equal(QbitValues.`|0> after H`)
      indexes(1) should equal(QbitValues.`|0> after X`)

      expectNoMsg()
    }
  }
}
