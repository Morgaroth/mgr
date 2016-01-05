package io.github.morgaroth.msc.quide.core.register

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import io.github.morgaroth.msc.quide.core.model.{X, I}
import io.github.morgaroth.msc.quide.core.model.QbitValue._
import io.github.morgaroth.msc.quide.core.monitoring.CompState.QbitState
import io.github.morgaroth.msc.quide.core.register.Qbit.{ReportValue, OperatorApply, Execute}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
  * Created by mateusz on 05.01.16.
  */
class QbitTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("QbitTest"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

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
