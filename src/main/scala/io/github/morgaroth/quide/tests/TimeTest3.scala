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

import scala.collection.immutable.Seq
import scala.compat.Platform
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}

/**
  * Created by morgaroth on 19.11.16.
  */
object TimeTest3 extends TestHelpers {
  implicit val tm: Timeout = 1.hour

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
    val problemSize = registerSize - 1
    val rounds = (math.Pi / 4 * registerSize).toInt
    val oracledValue = 2
    log.warning(s"grovering $oracledValue using $rounds rounds for size $registerSize")
    Helpers.usedMemKB
    Thread.sleep(5.seconds.toMillis)
    val initMemory = Helpers.usedMemKB
    reg.run(X, 0)
    reg.runWalsh()
    getValueFrom(reg)
    val start = Platform.currentTime
    reg.runOracle(oracledValue)
    reg.runInversion()
    getValueFrom(reg)
    val roundTime = Platform.currentTime - start
    saveVal("round-time", roundTime)
    saveVal("round-memory-usage", Helpers.usedMemKB - initMemory)
    log.error("stopping this shit")
    as.stop(reg.reg)
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