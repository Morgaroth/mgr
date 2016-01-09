package io.github.morgaroth.msc.quide.core.services

import akka.actor.ActorSystem
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import io.github.morgaroth.msc.quide.http.ExecuteOperatorReq
import io.github.morgaroth.msc.quide.model.QbitValue
import io.github.morgaroth.msc.quide.model.operators._
import io.github.morgaroth.msc.quide.core.monitoring.CompState
import io.github.morgaroth.msc.quide.core.monitoring.CompState.GetValue
import io.github.morgaroth.msc.quide.core.register.Register
import io.github.morgaroth.msc.quide.core.register.Register.{ExecuteOperator, ReportValue}
import spray.httpx.SprayJsonSupport
import spray.httpx.marshalling.ToResponseMarshallable
import spray.json.DefaultJsonProtocol
import spray.routing.Directives

import scala.concurrent.duration._

/**
  * Created by mateusz on 06.01.16.
  */
class CPUService(as: ActorSystem) extends Directives with marshallers with SprayJsonSupport {

  import as.dispatcher

  implicit val tm: Timeout = 10.seconds
  val log = Logging(as, getClass)

  override val pathEnd = pathEndOrSingleSlash

  val getCPUForId = as.actorOf(Register.props(5))

  val route =
    pathEnd {
      get(complete("hello from cpu service"))
    } ~
      pathPrefix("operators") {
        pathEnd {
          get(complete(List("H", "X", "Y", "Z", "I")))
        }
      } ~
      pathPrefix("test") {
        pathEnd {
          get(complete("hello from cpu service / test")) ~
            post(handleWith(handleOperation))
        }
      }

  def handleOperation(req: ExecuteOperatorReq): ToResponseMarshallable = {
    log.info(s"received execute req $req")
    val o = req.operator.toLowerCase match {
      case "h" | "hadammard" => H
      case "i" | "identity" => I
      case "x" | "paulix" => X
      case "y" | "pauliy" => Y
      case "z" | "pauliz" => Z
    }
    val a = as.actorOf(CompState.props(5))
    val result = (a ? GetValue).mapTo[Map[Int, QbitValue]].map(_.map(x => x._1.toString -> x._2))
    getCPUForId ! ExecuteOperator(o, req.index)
    getCPUForId ! ReportValue(a)
    result
  }
}


