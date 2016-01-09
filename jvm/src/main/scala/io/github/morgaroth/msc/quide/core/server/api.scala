package io.github.morgaroth.msc.quide.core.server

import akka.actor.{Actor, ActorContext, ActorLogging, Props}
import io.github.morgaroth.msc.quide.core.services.CPUService
import spray.http.StatusCodes.InternalServerError
import spray.http.{HttpEntity, StatusCode}
import spray.routing.RouteConcatenation
import spray.routing._
import spray.util.LoggingContext

import scala.util.control.NonFatal

/**
  * Created by mateusz on 06.01.16.
  */
trait WebApi extends RouteConcatenation with Directives {
  this: CoreActors with Core =>
  private implicit val _ = system.dispatcher


//  pathSingleSlash {
//    getFromFile("web/index.html")
//  }

  //@formatter:off
  val routes =
    pathEndOrSingleSlash {
      get(complete("Hello"))
    } ~
    pathPrefix("cpu") {
      new CPUService(system).route
    }
  //@formatter:on

  val rootService = system.actorOf(Props(new RoutedHttpService(routes)))
}

case class ErrorResponseException(responseStatus: StatusCode, response: Option[HttpEntity]) extends Exception

class RoutedHttpService(route: Route) extends Actor with HttpService with ActorLogging with CORSSupport {

  implicit def actorRefFactory: ActorContext = context

  implicit val handler = ExceptionHandler {
    case NonFatal(ErrorResponseException(statusCode, entity)) => ctx =>
      ctx.complete(statusCode, entity)

    case NonFatal(e) => ctx => {
      log.error(e, InternalServerError.defaultMessage)
      ctx.complete(InternalServerError)
    }
  }


  def receive: Receive =
    runRoute(cors(route))(handler, RejectionHandler.Default, context, RoutingSettings.default, LoggingContext.fromActorRefFactory)


}
