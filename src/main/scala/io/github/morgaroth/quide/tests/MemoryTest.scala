package io.github.morgaroth.quide.tests

import java.io.{File => JFile}

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import io.github.morgaroth.quide.core.model.Complex
import io.github.morgaroth.quide.core.model.gates.X
import io.github.morgaroth.quide.core.register.exc.RegisterExc
import io.github.morgaroth.quide.core.register.nodeath.RegisterNoDeaths
import io.github.morgaroth.quide.core.register.sync.RegisterSync

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.{implicitConversions, postfixOps}

/**
  * Created by morgaroth on 19.11.16.
  */
object MemoryTest extends TestHelpers {

  def main(args: Array[String]) {
    if (args.length != 2) sys.exit(-1)
    val size = args(1).toInt
    if (args(0) == "1") {
      val a = doMemoryGroverTest(size, RegisterNoDeaths.props)
      println(a)
    } else if (args(0) == "2") {
      val b = doMemoryGroverTest(size, RegisterSync.props)
      println(b)
    } else if (args(0) == "3") {
      val b = doMemoryGroverTest(size, RegisterExc.props)
      println(b)
    }
    Future.successful(List.empty[(String, Complex)])


    //    implicit val tm: Timeout = 1.hour
    //    implicit val as = ActorSystem("memorytest")
    //    import as.dispatcher
    //    val log = Logging(as, "test")
    //    val registerSize: Int = 4
    //    val reg = RegisterActions(as.actorOf(RegisterExc.props(registerSize)), registerSize)
    //    reg.run(X, 0)
    //    reg.runWalsh()
    //
    //    val oracledNumber: Int = 4
    //    reg.runOracle(oracledNumber)
    //    reg.runInversion()
    //    printValues("inversion 1", log, registerSize, reg)
    //    reg.runOracle(oracledNumber)
    //    reg.runInversion()
    //    val result = printValues("inversion 2", log, registerSize, reg)
    //    as.terminate().map(_ => result)
  }

  def doMemoryGroverTest(size: Int, registerToTest: Int => Props) = {
    val start = Helpers.checkUsedMemory
    val startTime = System.currentTimeMillis()
    implicit val tm: Timeout = 1.hour
    implicit val as = ActorSystem("memorytest")
    val rounds = math.round(math.Pi / 4 * math.sqrt(size)).toInt
    val reg = RegisterActions(as.actorOf(registerToTest(size)), size)
    val oracledValue = 3
    reg.run(X, 0)
    reg.runWalsh()
    val walshed = getValue(reg)
    val walshedTime = System.currentTimeMillis()
    val steps = (1 to rounds toList) map { _ =>
      reg.runOracle(oracledValue)
      reg.runInversion()
      (getValue(reg), System.currentTimeMillis())
    }
    val result = getValueFrom(reg)
    println(s"result is \n${result.mkString("\n")}")
    Await.result(as.terminate(), tm.duration)
    (start, 0) :: (walshed, walshedTime - startTime) :: steps.zipWithIndex.map {
      case ((mem, ts), idx) if idx == 0 => (mem, ts - walshedTime)
      case ((mem, ts), idx) => (mem, ts - steps(idx - 1)._2)
    }
  }

}
