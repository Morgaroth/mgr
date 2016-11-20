package io.github.morgaroth.quide.tests

import java.io.{File => JFile}

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.pattern.ask
import akka.util.Timeout
import better.files.Cmds._
import io.github.morgaroth.quide.core.model.QValue
import io.github.morgaroth.quide.core.model.gates.{ControlledGate, Gate, H, X}
import io.github.morgaroth.quide.core.monitoring.CompState
import io.github.morgaroth.quide.core.monitoring.CompState.{GetValue, Result}
import io.github.morgaroth.quide.core.register.QState.ReportValue
import io.github.morgaroth.quide.core.register.Register.ExecuteGate

import scala.concurrent.Await
import scala.language.{implicitConversions, postfixOps}

/**
  * Created by mateusz on 01.05.2016.
  */
trait TestHelpers {

  def saveValue(name: String, registerName: String, registerSize: Int, value: Double): Unit = {
    pwd / "data.data" << s"$name:${registerName.split('.').last}:$registerSize:$value"
  }

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


//object HeavyMemoryTest {
//
//  def main(args: Array[String]) {
//    val results = 1 to 1000 map { _ =>
//      val start = Platform.currentTime
//      val result = Await.result(MemoryTest.main(Array.empty), 10.seconds)
//      assert(result.count(_._2.modulus > 0.68) == 2, "BAD BAD BAD!")
//      Platform.currentTime - start
//    }
//    println((results.sum * 100.0 / results.size) / 100 toInt)
//  }
//}


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

  //  def runGrover(oracledValue: Int) = {
  //    val rounds = math.floor(math.Pi / 4 * math.sqrt(size)).toInt
  //    log.warning(s"grovering $oracledValue using $rounds rounds for size $size")
  //    run(X, 0)
  //    runWalsh()
  //    1 to rounds foreach { _ =>
  //      runOracle(oracledValue)
  //      runInversion()
  //    }
  //  }
}