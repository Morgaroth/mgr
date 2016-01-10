package io.github.morgaroth.msc.quide.front.api

import io.github.morgaroth.msc.quide.http.{CreateCPURes, ExecuteOperatorReq, CreateCPUReq}
import io.github.morgaroth.msc.quide.model.QbitValue
import org.scalajs.dom.ext.Ajax
import upickle.default._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.concurrent.Future

/**
  * Created by mateusz on 10.01.16.
  */
object Api {

  def createCPU(url: String, size: Int): Future[CreateCPURes] =
    Ajax.post(
      s"$url/cpu/",
      writeCreateCPU(CreateCPUReq(size)),
      headers = Map("Content-Type" -> "application/json")
    ).map(_.responseText).map(parseCreatedCPU)

  def getCPUState(url: String, cpuId: String): Future[Map[String, QbitValue]] =
    Ajax.get(s"$url/cpu/$cpuId")
      .map(x => read[Map[String, QbitValue]](x.responseText))


  def executeOperator(url: String, cpuId: String, operator: String, index: Int): Future[Map[String, QbitValue]] =
    Ajax.post(
      s"$url/cpu/$cpuId",
      writeExOperator(ExecuteOperatorReq(operator, index)),
      headers = Map("Content-Type" -> "application/json")
    ).map(x => read[Map[String, QbitValue]](x.responseText))

  def getOperatorList(url: String): Future[List[String]] =
    Ajax.get(s"$url/cpu/operators").map(x => read[List[String]](x.responseText))
}
