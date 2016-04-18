package io.github.morgaroth.msc.quide.core.actors

import akka.actor.{Actor, ActorLogging}
import io.github.morgaroth.utils.akka.WarnUnhandled

/**
  * Created by mateusz on 03.01.16.
  */
trait QuideActor extends Actor with ActorLogging with WarnUnhandled {
}
