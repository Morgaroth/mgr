package io.github.morgaroth.quide.tests

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
object TimeTest2 extends TestHelpers {
  implicit val tm: Timeout = 1.hour

  val registers: Map[String, Int => Props] = Map(
    "io.github.morgaroth.quide.core.register.own.RegisterOwn" -> RegisterOwn.props _,
    "io.github.morgaroth.quide.core.register.own_terminated.RegisterOwnTerminated" -> RegisterOwnTerminated.props _,
    "io.github.morgaroth.quide.core.register.nodeath.RegisterNoDeaths" -> RegisterNoDeaths.props _,
    "io.github.morgaroth.quide.core.register.custom_map.RegisterCustomMap" -> RegisterCustomMap.props _,
    "io.github.morgaroth.quide.core.register.sync.RegisterSync" -> RegisterSync.props _
  )


  val predefinedEffectiveRounds = Map(5 -> 9, 6 -> 4, 7 -> 6, 8 -> 8, 9 -> 11, 10 -> 16, 11 -> 22, 12 -> 32, 13 -> 45, 14 -> 63, 15 -> 89)

  def doTest(registerName: String, registerSize: Int) {
    def saveVal(name: String, value: Double) = saveValue(name, registerName, registerSize, value)

    implicit val as = ActorSystem("timetest")
    val reg = RegisterActions(as.actorOf(registers(registerName)(registerSize)), registerSize)
    val log = Logging(as, "test")
    log.warning("start")
    var execTime = 0L
    val problemSize = registerSize - 1
    val rounds = (math.Pi / 4 * registerSize).toInt
    val oracledValue = 2
    log.warning(s"grovering $oracledValue using $rounds rounds for size $registerSize")
    Helpers.usedMemKB
    Thread.sleep(5.seconds.toMillis)
    val initMemory = Helpers.usedMemKB
    var start = Platform.currentTime
    reg.run(X, 0)
    reg.runWalsh()
    println(getValueFrom(reg).toList.sortBy(_._2.modulus).takeRight(4).map(x => x._1 -> x._2.asString))
    var end = true
    var roundsEffecctive = 0
    execTime += (Platform.currentTime - start)
    while (end) {
      start = Platform.currentTime
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
    execTime += (Platform.currentTime - start)
    val propSum = values.map(x => x._2.modulus * x._2.modulus).sum
    log.warning(s"${values.takeRight(5).toString} results ${values.size}, propSum $propSum")
    log.error("stopping this shit")
    as.stop(reg.reg)
    saveVal("execution-time-total", execTime)
    Await.result(as.terminate(), 1 minute)
  }

  def main(args: Array[String]) {
    val registerName = args.head
    val sizes: Iterable[Int] = if (args.tail.nonEmpty) args.tail.map(_.toInt) else 15 to 15 toList

    sizes map { size =>
      size -> doTest(registerName, size)
    }
  }
}
