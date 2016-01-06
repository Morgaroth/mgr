package io.github.morgaroth.msc.quide.core.register

import akka.actor.{ActorSystem, ActorRef}
import io.github.morgaroth.msc.quide.core.BaseQuideActorTest
import io.github.morgaroth.msc.quide.core.model.QbitValue._
import io.github.morgaroth.msc.quide.core.model.{I, X}
import io.github.morgaroth.msc.quide.core.monitoring.CompState.QbitState
import io.github.morgaroth.msc.quide.core.register.Qbit.{Execute, OperatorApply, ReportValue}

/**
  * Created by mateusz on 05.01.16.
  */
class QbitTest(as: ActorSystem) extends BaseQuideActorTest(as) {

  def this() = this(ActorSystem("QbitTest"))

  def withActor(code: ActorRef => Unit) = {
    code(system.actorOf(Qbit.props(0, `|0>`)))
  }

  "Qbit actor" when {
    "receive report vale to should send value to provided collector" in withActor { actor =>
      actor ! Execute(ReportValue(testActor), 0)
      expectMsgClass(classOf[QbitState])
    }

    "set to |0>" must {
      "report value |0> after send I" in withActor { actor =>
        actor ! Execute(OperatorApply(I), 0)
        actor ! Execute(ReportValue(testActor), 1)
        val a = expectMsgClass(classOf[QbitState])
        a.value should equal(`|0>`)
      }
      "report value |1> after send Pauli X gate (negate)" in withActor { actor =>
        actor ! Execute(OperatorApply(X), 0)
        actor ! Execute(ReportValue(testActor), 1)
        val a = expectMsgClass(classOf[QbitState])
        a.value should equal(`|1>`)
      }
    }
  }
}
