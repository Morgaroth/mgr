package io.github.morgaroth.quide.tests

//import java.io.{File => JFile}

import akka.actor.{ActorSystem, Props}
import akka.event.Logging
import akka.util.Timeout
import io.github.morgaroth.quide.core.model.QValue
import io.github.morgaroth.quide.core.model.gates.X
import io.github.morgaroth.quide.core.register.custom_map.RegisterCustomMap
import io.github.morgaroth.quide.core.register.nodeath.RegisterNoDeaths
import io.github.morgaroth.quide.core.register.own.RegisterOwn
import io.github.morgaroth.quide.core.register.own_terminated.RegisterOwnTerminated
import io.github.morgaroth.quide.core.register.sync.RegisterSync

import scala.compat.Platform
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}

/**
  * Created by morgaroth on 19.11.16.
  */
object TimeTest extends TestHelpers {
  implicit val tm: Timeout = 2.minutes

  val registers: Map[String, Int => Props] = Map(
    "io.github.morgaroth.quide.core.register.own.RegisterOwn" -> RegisterOwn.props _,
    "io.github.morgaroth.quide.core.register.own_terminated.RegisterOwnTerminated" -> RegisterOwnTerminated.props _,
    "io.github.morgaroth.quide.core.register.nodeath.RegisterNoDeaths" -> RegisterNoDeaths.props _,
    "io.github.morgaroth.quide.core.register.custom_map.RegisterCustomMap" -> RegisterCustomMap.props _,
    "io.github.morgaroth.quide.core.register.sync.RegisterSync" -> RegisterSync.props _
  )

  def doTest(registerName: String, registerSize: Int) {
    def saveVal(name: String, value: Double) = saveValue(name, registerName, registerSize, value)

    implicit val as = ActorSystem("timetest")
    val reg = RegisterActions(as.actorOf(registers(registerName)(registerSize)), registerSize)
    val log = Logging(as, "test")
    log.warning("start")
    var start = Platform.currentTime
    var execTime = 0L
    val problemSize = registerSize - 1

    val rounds = (math.Pi / 4 * registerSize).toInt
    //      val rounds = problemSize * 2
    val oracledValue = 2
    log.warning(s"grovering $oracledValue using $rounds rounds for size $registerSize")
    Helpers.usedMemKB
    Thread.sleep(5.seconds.toMillis)
    val initMemory = Helpers.usedMemKB
    reg.run(X, 0)
    reg.runWalsh()
    //      val walshed = getValue(reg)
    //      val walshedTime = System.currentTimeMillis()
    println(getValueFrom(reg).toList.sortBy(_._2.modulus).takeRight(4).map(x => x._1 -> x._2.asString))

    //      (0 to rounds toList) foreach { _ =>
    //        reg.runOracle(oracledValue)
    //        reg.runInversion()
    //        println(getValueFrom(reg).toList.sortBy(_._2.modulus).takeRight(4).toString)
    //      }

    //      (0 to rounds toList) foreach { _ =>
    var end = true
    var roundsEffecctive = 0
    execTime += (Platform.currentTime - start)
    while (end) {
      val start = Platform.currentTime
      roundsEffecctive += 1
      reg.runOracle(oracledValue)
      reg.runInversion()
      val values = getValueFrom(reg).toList.sortBy(_._2.modulus)
      val roundTime = Platform.currentTime - start
      execTime += roundTime
      saveVal("round-time", roundTime)
      saveVal("round-memory-usage", Helpers.usedMemKB - initMemory)
      if (values.takeRight(2).map(_._2.modulus).map(x => x * x).sum > 0.97) {
        log.warning(s"End, effective rounds $roundsEffecctive for problem qbits $problemSize")
        saveVal("effective-rounds", roundsEffecctive)
        end = false
      }
      println(values.takeRight(4).map(x => (x._1, x._2.toString())))
    }
    start = Platform.currentTime
    val values: List[(String, QValue, Double)] = getValueFrom(reg).toList.sortBy(_._2.modulus).map(x => (x._1, x._2, (x._2.modulus * x._2.modulus * 10000).toInt / 100.0))
    //      values.foreach(println)
    val propSum = values.map(x => x._2.modulus * x._2.modulus).sum
    log.warning(s"${values.takeRight(5).toString} results ${values.size}, propSum $propSum")
    execTime += (Platform.currentTime - start)
    log.error("stopping this shit")
    //      log.error(s"stopping ${Await.result(gracefulStop(reg.reg, 4 minutes), 5 minutes)}")
    as.stop(reg.reg)
    saveVal("execution-time", execTime)
    Await.result(as.terminate(), 1 minute)
    //    times * 1.0 / 5
  }

  def main(args: Array[String]) {
    val registerName = args.head
    val sizes: Iterable[Int] = if (args.tail.nonEmpty) args.tail.map(_.toInt) else 15 to 15 toList

//    1 to 5 map { _ =>
      sizes map { size =>
        size -> doTest(registerName, size)
      }
//    }
  }
}
