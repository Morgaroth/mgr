package io.github.morgaroth.msc.quide.core.services

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import io.github.morgaroth.msc.quide.core.monitoring.CompState
import io.github.morgaroth.msc.quide.core.monitoring.CompState.GetValue
import io.github.morgaroth.msc.quide.core.register.Register
import io.github.morgaroth.msc.quide.core.register.Register.{ExecuteGate, ReportValue}
import io.github.morgaroth.msc.quide.http.{CPU, CreateCPUReq, ExecuteOperatorReq}
import io.github.morgaroth.msc.quide.model.QValue
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport
import spray.httpx.marshalling.ToResponseMarshallable
import spray.routing.Directives

import scala.collection.mutable
import scala.concurrent.duration._

/**
  * Created by mateusz on 06.01.16.
  */
class CPUService(as: ActorSystem) extends Directives with marshallers with SprayJsonSupport {

  import as.dispatcher

  implicit val tm: Timeout = 10.seconds
  val log = Logging(as, getClass)

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

  def handleOperation(id: UUID, userId: String)(req: ExecuteOperatorReq): ToResponseMarshallable = {
    log.info(s"received execute req $req on cpu $id")
    cpusPU.get(userId).flatMap(_.get(id)) map[ToResponseMarshallable] { case (register, s) =>
      register ! ExecuteGate(req.operator, req.index)
      akka.pattern.after(200.millis, as.scheduler) {
        val listener = as.actorOf(CompState.props(s.size))
        val result = (listener ? GetValue).mapTo[Map[String, QValue]]
        register ! ReportValue(listener)
        result
      }
    } getOrElse StatusCodes.BadRequest
  }

  def createNewCPU(userIdMaybe: String)(req: CreateCPUReq): ToResponseMarshallable = {
    log.info("creating cpu")
    val id = UUID.randomUUID()
    val ref = as.actorOf(Register.props(req.size))
    val resp = CPU(req.size, id.toString)
    cpusPU.getOrElseUpdate(userIdMaybe, mutable.Map.empty).update(id, (ref, resp))
    resp
  }

  def handleCPUDeletion(cpuId: UUID, userId: String) = {
    cpusPU.get(userId).flatMap(_.get(cpuId)).foreach(_._1 ! PoisonPill)
    cpusPU.get(userId).foreach(_.remove(cpuId))
    StatusCodes.NoContent
  }

  def getCPUValue(cpuId: UUID, userId: String): ToResponseMarshallable = {
    cpusPU.get(userId).flatMap(_.get(cpuId)) map[ToResponseMarshallable] { case (register, s) =>
      val listener = as.actorOf(CompState.props(s.size))
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


