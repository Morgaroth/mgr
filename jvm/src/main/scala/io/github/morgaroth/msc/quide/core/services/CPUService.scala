package io.github.morgaroth.msc.quide.core.services

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout
import io.github.morgaroth.msc.quide.core.monitoring.CompState
import io.github.morgaroth.msc.quide.core.monitoring.CompState.GetValue
import io.github.morgaroth.msc.quide.core.register.Register
import io.github.morgaroth.msc.quide.core.register.Register.{ExecuteOperator, ReportValue}
import io.github.morgaroth.msc.quide.http.{CreateCPUReq, CPU, ExecuteOperatorReq}
import io.github.morgaroth.msc.quide.model.QValue
import io.github.morgaroth.msc.quide.model.operators._
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport
import spray.httpx.marshalling.ToResponseMarshallable
import spray.routing.Directives

import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
  * Created by mateusz on 06.01.16.
  */
class CPUService(as: ActorSystem) extends Directives with marshallers with SprayJsonSupport {

  import as.dispatcher

  implicit val tm: Timeout = 10.seconds
  val log = Logging(as, getClass)

  override val pathEnd = pathEndOrSingleSlash

  val cpus = mutable.Map.empty[UUID, (ActorRef, CPU)]

  //@formatter:off
  val route =
    pathEnd {
      get(complete(getAvailableCPUS)) ~
        post(handleWith(createNewCPU))
    } ~
      pathPrefix(JavaUUID) { cpuId =>
        pathEnd {
          get(complete(getCPUValue(cpuId))) ~
            post(handleWith(handleOperation(cpuId)))
        }
      } ~
      pathPrefix("operators") {
        pathEnd {
          get(complete(List("H", "X", "Y", "Z", "I")))
        }
      }
  //@formatter:on

  def handleOperation(id: UUID)(req: ExecuteOperatorReq): ToResponseMarshallable = {
    log.info(s"received execute req $req on cpu $id")
    val o = req.operator.toLowerCase match {
      case "h" | "hadammard" => H
      case "i" | "identity" => I
      case "x" | "paulix" => X
      case "y" | "pauliy" => Y
      case "z" | "pauliz" => Z
    }
    cpus.get(id) map[ToResponseMarshallable] { case (register, s) =>
      register ! ExecuteOperator(o, req.index)
      akka.pattern.after(1.second, as.scheduler){
        val listener = as.actorOf(CompState.props(s.size))
        val result = (listener ? GetValue).mapTo[Map[String, QValue]]
        register ! ReportValue(listener)
        result
      }
    } getOrElse StatusCodes.BadRequest
  }

  def createNewCPU(req: CreateCPUReq): ToResponseMarshallable = {
    log.info("createing cpu")
    val id = UUID.randomUUID()
    val ref = as.actorOf(Register.props(req.size))
    val resp = CPU(req.size, id.toString)
    cpus += id ->(ref, resp)
    resp
  }

  def getCPUValue(cpuId: UUID): ToResponseMarshallable = {
    cpus.get(cpuId) map[ToResponseMarshallable] { case (register, s) =>
      val listener = as.actorOf(CompState.props(s.size))
      val result = (listener ? GetValue).mapTo[Map[String, QValue]]
      register ! ReportValue(listener)
      result
    } getOrElse StatusCodes.BadRequest
  }


  def getAvailableCPUS: ToResponseMarshallable = {
    cpus.valuesIterator.map(_._2).toList
  }

}


