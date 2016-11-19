package io.github.morgaroth.quide.tests

import java.io.{File => JFile}

import akka.actor.{ActorSystem, Props}
import akka.event.Logging
import akka.util.Timeout
import better.files.Cmds._
import io.github.morgaroth.quide.core.model.QValue
import io.github.morgaroth.quide.core.model.gates.X
import io.github.morgaroth.quide.core.register.own.RegisterOwn
import io.github.morgaroth.quide.core.register.own_terminated.sync.RegisterOwnTerminated
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
    "io.github.morgaroth.quide.core.register.own_terminated.sync.RegisterOwnTerminated" -> RegisterOwnTerminated.props _,
    "io.github.morgaroth.quide.core.register.sync.RegisterSync" -> RegisterSync.props _
  )

  def doTest(registerName: String, registerSize: Int): Double = {
    implicit val as = ActorSystem("timetest")
    //    val times: Long = 1 to 5 map { _ =>
    val times = {
      val reg = RegisterActions(as.actorOf(registers(registerName)(registerSize)), registerSize)
      val log = Logging(as, "test")
      log.warning("start")
      val start = Platform.currentTime
      val problemSize = registerSize - 1

      val rounds = (math.Pi / 4 * math.sqrt(problemSize)).toInt
      val oracledValue = 2
      log.warning(s"grovering $oracledValue using $rounds rounds for size $registerSize")
      reg.run(X, 0)
      reg.runWalsh()
      //      val walshed = getValue(reg)
      //      val walshedTime = System.currentTimeMillis()
      val steps = (1 to rounds toList) foreach { _ =>
        reg.runOracle(oracledValue)
        reg.runInversion()
        //        (getValue(reg), System.currentTimeMillis())
      }


      val values: List[(String, QValue)] = getValueFrom(reg).toList.sortBy(_._2.modulus)
      //      values.foreach(println)
      log.warning(s"${values.takeRight(5).toString} results ${values.size}")
      val time = Platform.currentTime - start
      log.error("stopping this shit")
      //      log.error(s"stopping ${Await.result(gracefulStop(reg.reg, 4 minutes), 5 minutes)}")
      as.stop(reg.reg)
      time
      //    } sum
    }

    Await.result(as.terminate(), 1 minute)
    //    times * 1.0 / 5
    times
  }

  def main(args: Array[String]) {
    println(args.toList)
    val registerName = args.head
    val sizes: Iterable[Int] = if (args.tail.nonEmpty) args.tail.map(_.toInt) else 15 to 15 toList
    val results = sizes map { size =>
      size -> doTest(registerName, size)
    }
    pwd / "data.d" << results.map(x => s"${x._1}, ${x._2}ms").mkString("\n")
  }
}
