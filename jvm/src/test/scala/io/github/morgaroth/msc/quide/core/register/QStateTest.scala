package io.github.morgaroth.msc.quide.core.register

import akka.actor.{ActorSystem, DeadLetter}
import akka.testkit._
import io.github.morgaroth.msc.quide.core.monitoring.CompState.StateAmplitude
import io.github.morgaroth.msc.quide.core.register.QState.{Execute, GateApply, MyAmplitude, ReportValue}
import io.github.morgaroth.msc.quide.model._
import io.github.morgaroth.msc.quide.model.gates.H
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
  * Created by mateusz on 07.03.16.
  */
class QStateTest() extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "An QState actor" when {
    "register is 1-qbit" should {
      "look for opposite state when executing 1-qbit operator" in {
        testLookForCorrectOpposite("1", "0", 0)
        testLookForCorrectOpposite("0", "1", 0)
      }
      "execute hadammard gate correctly" in {
        checkIfExecuteHadammardCorrectly("0", 0, QValue.`1/p2`)
        checkIfExecuteHadammardCorrectly("1", 0, QValue.`-1/p2`)
      }
    }
    "register is 2-qbit" should {
      "look for opposite state when executing 1-qbit operator" in {
        testLookForCorrectOpposite("00", "01", 0)
        testLookForCorrectOpposite("00", "10", 1)
      }
    }
    "register is 2-qbit" should {
      "look for opposite state when executing 1-qbit operator on boundary qbits" in {
        testLookForCorrectOpposite("000", "001", 0)
        testLookForCorrectOpposite("100", "000", 2)
      }
      "look for opposite state when executing 1-qbit operator on internal qbits" in {
        testLookForCorrectOpposite("10101110", "10111110", 4)
        testLookForCorrectOpposite("1110001", "1111001", 3)
      }
    }
  }

  def checkIfExecuteHadammardCorrectly(state: String, qbitNo: Int, expectedAmplitude: QValue): Unit = {
    val test = system.actorOf(QState.props(QValue.`1`), state)
    val reporter = TestProbe()
    test ! Execute(GateApply(H, qbitNo), 0)
    test ! MyAmplitude(0)
    test ! Execute(ReportValue(reporter.ref), 1)
    val a: QValue = reporter.expectMsgClass(classOf[StateAmplitude]).value
    a should equal(expectedAmplitude)
    system.stop(test)
  }

  def testLookForCorrectOpposite(initState: String, oppositeRef: String, qbitNo: Int): Unit = {
    val testDeadLetter = TestProbe()
    system.eventStream.subscribe(testDeadLetter.ref, classOf[DeadLetter])
    val test = system.actorOf(QState.props(), initState)
    test ! Execute(GateApply(H, qbitNo), 0)
    val DeadLetter(msg, from, to) = testDeadLetter.expectMsgClass(classOf[DeadLetter])
    msg should equal(MyAmplitude(Complex.`0`))
    from should equal(test)
    to.path.name should equal(oppositeRef)
    system.eventStream.unsubscribe(testDeadLetter.ref)
    system.stop(test)
  }
}