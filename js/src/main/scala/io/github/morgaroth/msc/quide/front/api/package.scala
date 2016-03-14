package io.github.morgaroth.msc.quide.front

import io.github.morgaroth.msc.quide.http.{CreateCPUReq, CPU, ExecuteOperatorReq}

/**
  * Created by mateusz on 07.01.16.
  */
package object api {

  import upickle.default._

  val parseCreatedCPU = read[CPU] _
  val writeCreateCPU = write[CreateCPUReq](_: CreateCPUReq, 0)
  val writeExOperator = write[ExecuteOperatorReq](_: ExecuteOperatorReq, 0)
}
