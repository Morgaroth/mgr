package io.github.morgaroth.quide.utils

import akka.actor.ActorSystem
import io.github.morgaroth.quide.core.monitoring.CompState
import io.github.morgaroth.quide.core.monitoring.CompState.States

import scala.concurrent.duration._

/**
  * Created by morgaroth on 19.12.16.
  */
object ExceptionTester {
  def main(args: Array[String]): Unit = {
    val s = ActorSystem("test")

    val a1 = s.actorOf(CompState.props(10, Some(1.hour)), "test")
    a1 ! States(126)
    println(a1, "ready")
    Thread.sleep(1000)
    Thread.`yield`()
    try {
      val a2 = s.actorOf(CompState.props(10, Some(1.hour)), "test")
      a2 ! States(256)
      println(a2, "ready")
    } catch {
      case t: Throwable =>
        print(s"catched t $t of class ${t.getClass.getCanonicalName}")
    }
    Thread.sleep(1000 * 120)
  }
}
