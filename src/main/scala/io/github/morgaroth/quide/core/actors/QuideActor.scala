package io.github.morgaroth.quide.core.actors

import akka.actor.{Actor, ActorLogging, ActorSelection}
import io.github.morgaroth.quide.core.model.QValue
import io.github.morgaroth.utils.akka.WarnUnhandled

/**
  * Created by mateusz on 03.01.16.
  */
trait QuideActor extends Actor with ActorLogging with WarnUnhandled {
  var myName: String = _

  def loginfo(msg: String) = log.info(s"|$myName> - $msg")
}

trait QStateBase extends QuideActor {
  val register = self.path.parent
  val parent = context.parent

  var amplitude: QValue = _
  var currentNo: Long = -1
  val deadAmplitude = 0.0001d

  def findOpposedState(index: Int): (Char, String) = {
    if (index >= myName.length) throw new RuntimeException(s"illegal size index $index my name $myName, ${myName.length}")
    val myQbit: Char = myName(myName.length - index - 1)
    val opposedQbit = if (myQbit == '0') '1' else '0'
    (myQbit, "%s%c%s".format(myName.slice(0, myName.length - index - 1), opposedQbit, myName.slice(myName.length - index, myName.length)))
  }


  //
//    def ShallIDead() = {
//      if (amplitude <= deadAmplitude) {
//        context stop self
//        true
//      } else false
//    }

  def ShallIDead() = false
}

trait QStateActor extends QStateBase {
  // constructor arguments
  val init: QValue
  val startNo: Long

  // local
  myName = self.path.name
}