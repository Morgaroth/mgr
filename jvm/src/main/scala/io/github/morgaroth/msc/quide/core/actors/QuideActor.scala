package io.github.morgaroth.msc.quide.core.actors

import akka.actor.{Actor, ActorLogging}
import io.github.morgaroth.msc.quide.model._
import io.github.morgaroth.utils.akka.WarnUnhandled

/**
  * Created by mateusz on 03.01.16.
  */
trait QuideActor extends Actor with ActorLogging
//  with WarnUnhandled

trait QStateActor extends QuideActor {
  // constructor arguments
  val init: QValue
  val startNo: Long

  // local
  val register = self.path.parent
  val parent = context.parent
  val myName = self.path.name
  val deadAmplitude = 0.001d
  var amplitude: QValue = init
  var currentNo = startNo

  def findOpposedState(index: Int): (Char, String) = {
    val myQbit: Char = myName(myName.length - index - 1)
    val oponentQbit = if (myQbit == '0') '1' else '0'
    (myQbit, "%s%c%s".format(myName.slice(0, myName.length - index - 1), oponentQbit, myName.slice(myName.length - index, myName.length)))
  }

  def loginfo(msg: String) = {
    log.info(s"|$myName> - $msg")
  }

  def ShallIDead() = {
    if (amplitude <= deadAmplitude) {
      context stop self
      true
    } else false
  }
}