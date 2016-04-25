package io.github.morgaroth.msc.quide.core.services

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import io.github.morgaroth.msc.quide.core.monitoring.CompState
import io.github.morgaroth.msc.quide.core.monitoring.CompState.GetValue
import io.github.morgaroth.msc.quide.core.register.Register.{ExecuteGate, ReportValue}
import io.github.morgaroth.msc.quide.core.register.doesntwork.RegisterDoesntWork
import io.github.morgaroth.msc.quide.core.register.nodeath.RegisterNoDeaths
import io.github.morgaroth.msc.quide.core.register.sync.RegisterSync
import io.github.morgaroth.msc.quide.http.{CPU, CreateCPUReq, ExecuteOperatorReq}
import io.github.morgaroth.msc.quide.model.QValue
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport
import spray.httpx.marshalling.ToResponseMarshallable
import spray.routing.Directives

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Created by mateusz on 06.01.16.
  */
class CPUService(as: ActorSystem) extends Directives with marshallers with SprayJsonSupport {

  implicit val tm: Timeout = 60.seconds
  val log = Logging(as, getClass)
  implicit val ex: ExecutionContext = as.dispatcher

  override val pathEnd = pathEndOrSingleSlash

  val cpusPU = mutable.Map.empty[String, mutable.Map[UUID, (ActorRef, CPU)]]

  //@formatter:off
  val route =
    headerValueByName("X-User-Id") { userId =>
      pathEnd {
        get(complete(getAvailableCPUS(userId))) ~
        post(handleWith(createNewCPU(userId)))
      } ~
      pathPrefix(JavaUUID) { cpuId =>
        pathEnd {
          get(complete(getCPUValue(cpuId, userId))) ~
          post(handleWith(handleOperation(cpuId, userId))) ~
          delete(complete(handleCPUDeletion(cpuId, userId)))
        }
      }
    }
  //@formatter:on

  def handleOperation(id: UUID, userId: String)(request: List[ExecuteOperatorReq]): ToResponseMarshallable = {
    log.info(s"received execute req $request on cpu $id")
    cpusPU.get(userId).flatMap(_.get(id)) map[ToResponseMarshallable] { case (register, s) =>
      request.foreach { req =>
        register ! ExecuteGate(req.gate, req.index)
      }
      StatusCodes.OK
    } getOrElse StatusCodes.BadRequest
  }

  def createNewCPU(userIdMaybe: String)(req: CreateCPUReq): ToResponseMarshallable = {
    log.info(s"creating cpu req $req")
    val id = UUID.randomUUID()
    val props = req.`type` match {
      case "nodeaths" => RegisterNoDeaths.props(req.size)
      case "sync" => RegisterSync.props(req.size)
      case _ => RegisterDoesntWork.props(req.size)
    }
    val ref = as.actorOf(props)
    val resp = CPU(req.size, id.toString)
    cpusPU.getOrElseUpdate(userIdMaybe, mutable.Map.empty).update(id, (ref, resp))
    resp
  }

  def handleCPUDeletion(cpuId: UUID, userId: String): ToResponseMarshallable = {
    cpusPU.get(userId).flatMap(_.get(cpuId)).foreach(_._1 ! PoisonPill)
    cpusPU.get(userId).foreach(_.remove(cpuId))
    StatusCodes.NoContent
  }

  def getCPUValue(cpuId: UUID, userId: String): ToResponseMarshallable = {
    cpusPU.get(userId).flatMap(_.get(cpuId)) map[ToResponseMarshallable] { case (register, s) =>
      val listener = as.actorOf(CompState.props(Math.pow(2, s.size).toLong))
      val result = (listener ? GetValue).mapTo[Map[String, QValue]]
      register ! ReportValue(listener)
      result
    } getOrElse StatusCodes.BadRequest
  }


  def getAvailableCPUS(userId: String): ToResponseMarshallable = {
    val flatten: List[CPU] = cpusPU.get(userId).map(_.valuesIterator.map(_._2).toList).toList.flatten
    flatten
  }
}


