package io.github.morgaroth.msc.quide.front.api

import io.github.morgaroth.msc.quide.http.{CPU, CreateCPUReq, ExecuteOperatorReq}
import io.github.morgaroth.msc.quide.model.QValue
import io.github.morgaroth.msc.quide.model.gates.{Gate, SingleQbitGate}
import org.scalajs.dom.ext.Ajax
import upickle.default._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by mateusz on 10.01.16.
  */
object Api {

  def createCPU(url: String, size: Int): Future[CPU] =
    Ajax.post(
      s"$url/cpu/",
      writeCreateCPU(CreateCPUReq(size)),
      headers = Map("Content-Type" -> "application/json")
    ).map(_.responseText).map(parseCreatedCPU)

  def getCPUState(url: String, cpuId: String): Future[Map[String, QValue]] =
    Ajax.get(s"$url/cpu/$cpuId")
      .map(x => read[Map[String, QValue]](x.responseText))

  def listCPUs(url: String) =
    Ajax.get(s"$url/cpu/").map { x =>
      read[List[CPU]](x.responseText)
    }

  def executeOperator(url: String, cpuId: String, operator: Gate, index: Int): Future[Map[String, QValue]] =
    Ajax.post(
      s"$url/cpu/$cpuId",
      writeExOperator(ExecuteOperatorReq(operator, index)),
      headers = Map("Content-Type" -> "application/json")
    ).map(x => read[Map[String, QValue]](x.responseText))

  def getOperatorList(url: String): Future[List[SingleQbitGate]] =
    Ajax.get(s"$url/cpu/operators").map(x => read[List[SingleQbitGate]](x.responseText))
}
