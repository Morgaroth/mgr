package io.github.morgaroth.quide.tests

import java.io.{Serializable, File => JFile}

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.pattern.ask
import akka.util.Timeout
import better.files.Cmds._
import better.files._
import io.github.morgaroth.quide.core.model.gates.{ControlledGate, Gate, H, X}
import io.github.morgaroth.quide.core.model.{Complex, QValue}
import io.github.morgaroth.quide.core.monitoring.CompState
import io.github.morgaroth.quide.core.monitoring.CompState.{GetValue, Result}
import io.github.morgaroth.quide.core.register.QState.ReportValue
import io.github.morgaroth.quide.core.register.Register.ExecuteGate
import io.github.morgaroth.quide.core.register.exc.RegisterExc
import io.github.morgaroth.quide.core.register.nodeath.RegisterNoDeaths
import io.github.morgaroth.quide.core.register.sync.RegisterSync

import scala.compat.Platform
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.{implicitConversions, postfixOps}

/**
  * Created by mateusz on 01.05.2016.
  */


trait TestHelpers {
  def printValues(id: String, log: LoggingAdapter, reg: RegisterActions)(implicit t: Timeout, as: ActorSystem) = {
    val data: List[(String, QValue)] = getValueFrom(reg).toList.sortBy(_._1)
    log.info(s"result $id is \n${data.map(x => s"${x._1}: ${x._2.pretty}  -- ${List.fill(x._2.modulus * 10 toInt)('*').mkString}").mkString("\n")}")
    data
  }

  def getValue(reg: RegisterActions)(implicit tm: Timeout, as: ActorSystem) = {
    getValueFrom(reg)
    Helpers.checkUsedMemory
  }

  def getValueFrom(reg: RegisterActions)(implicit tm: Timeout, as: ActorSystem): Result = {
    val report1 = as.actorOf(CompState.props(reg.size, None))
    val reportF = (report1 ? GetValue).mapTo[Result]
    reg ! ReportValue(report1)
    Await.result(reportF, tm.duration)
  }
}

object TimeTest extends TestHelpers {
  implicit val tm: Timeout = 20.seconds

  import akka.pattern._

  def doTest(registerSize: Int): Double = {
    implicit val as = ActorSystem("memorytest")
    val times: Long = 1 to 5 map { _ =>
      val reg = RegisterActions(as.actorOf(RegisterExc.props(registerSize)), registerSize)
      val log = Logging(as, "test")
      log.warning("start")
      val start = Platform.currentTime
      reg.runGrover(1)
      log.warning(getValueFrom(reg).toList.sortBy(_._2.modulus).takeRight(2).toString)
      val time = Platform.currentTime - start
      log.error("stopping this shit")
//      log.error(s"stopping ${Await.result(gracefulStop(reg.reg, 4 minutes), 5 minutes)}")
      time
    } sum

    Await.result(as.terminate(), 1 minute)
    times * 1.0 / 5
  }

  def main(args: Array[String]) {
    val sizes: Iterable[Int] = if (args.nonEmpty) args.map(_.toInt) else 15 to 15 toList
    val results = sizes map { size =>
      size -> doTest(size)
    }
    pwd / "data.d" << results.map(x => s"${x._1}, ${x._2}ms").mkString("\n")
  }
}

object HeavyMemoryTest {

  def main(args: Array[String]) {
    val results = 1 to 1000 map { _ =>
      val start = Platform.currentTime
      val result = Await.result(MemoryTest.main(Array.empty), 10.seconds)
      assert(result.count(_._2.modulus > 0.68) == 2, "BAD BAD BAD!")
      Platform.currentTime - start
    }
    println((results.sum * 100.0 / results.size) / 100 toInt)
  }
}


object MemoryTest extends TestHelpers {

  def main(args: Array[String]) = {
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
    (start, 0) ::(walshed, walshedTime - startTime) :: steps.zipWithIndex.map {
      case ((mem, ts), idx) if idx == 0 => (mem, ts - walshedTime)
      case ((mem, ts), idx) => (mem, ts - steps(idx - 1)._2)
    }
  }

}

object Helpers {
  def checkUsedMemory = {
    System.gc()
    System.gc()
    System.gc()
    System.gc()
    System.gc()
    System.gc()
    val r = Runtime.getRuntime
    val l: Long = r.totalMemory() - r.freeMemory()
    s"(${l / 1024}kB, ${l / 1024 / 1024}MB)"
  }
}

case class RegisterActions(reg: ActorRef, size: Int)(implicit as: ActorSystem) {
  val register = 0 until size
  val bank = 1 until size
  val log = Logging(as, "actions")

  def !(msg: Any) = reg ! msg

  def run(g: Gate, qbits: Iterable[Int]): Unit = qbits.foreach(idx => reg ! ExecuteGate(g, idx))

  def run(g: Gate, i: Int): Unit = run(g, Seq(i))

  def runWalsh(): Unit = run(H, 0 until size)

  def runOracle(oracledNumber: Int): Unit = {
    if (oracledNumber > math.pow(2, size - 1) - 1) {
      throw new RuntimeException("cannot")
    }
    val revertBits = Integer.toBinaryString(oracledNumber).zipWithIndex.filter(_._1 == '0').map(_._2 + 1).toSet
    run(X, revertBits)
    run(ControlledGate(X, 1 until size), 0)
    run(X, revertBits)
  }


  def runInversion(): Unit = {
    run(H, bank)
    run(X, bank)
    run(H, 1)
    run(ControlledGate(X, 2 until size), 1)
    run(H, 1)
    run(X, bank)
    run(H, bank)
  }

  def runGrover(oracledValue: Int) = {
    val rounds = math.floor(math.Pi / 4 * math.sqrt(size)).toInt
    log.warning(s"grovering $oracledValue using $rounds rounds for size $size")
    run(X, 0)
    runWalsh()
    1 to rounds foreach { _ =>
      runOracle(oracledValue)
      runInversion()
    }
  }
}