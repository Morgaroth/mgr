package io.github.morgaroth.msc.quide.core.services

import akka.actor.ActorSystem
import akka.event.Logging
import akka.util.Timeout
import io.github.morgaroth.msc.quide.core.model._
import io.github.morgaroth.msc.quide.core.monitoring.CompState
import io.github.morgaroth.msc.quide.core.monitoring.CompState.GetValue
import io.github.morgaroth.msc.quide.core.register.Register
import io.github.morgaroth.msc.quide.core.register.Register.{ReportValue, ExecuteOperator}
import spray.httpx.SprayJsonSupport
import spray.httpx.marshalling.ToResponseMarshallable
import spray.json.DefaultJsonProtocol
import spray.routing.Directives
import us.bleibinha.spray.json.macros.lazyy.json
import akka.pattern.ask
import scala.concurrent.duration._

/**
  * Created by mateusz on 06.01.16.
  */
class CPUService(as: ActorSystem) extends Directives with DefaultJsonProtocol with SprayJsonSupport {

  import as.dispatcher

  implicit val tm: Timeout = 10.seconds
  val log = Logging(as, getClass)

  override val pathEnd = pathEndOrSingleSlash

  val getCPUForId = as.actorOf(Register.props(5))

  val route =
    pathEnd {
      get(complete("hello from cpu service"))
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

@json case class ExecuteOperatorReq(operator: String, index: Int)
