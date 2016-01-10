package io.github.morgaroth.msc.quide.front

import io.github.morgaroth.msc.quide.http.{CreateCPUReq, CreateCPURes, ExecuteOperatorReq}

/**
  * Created by mateusz on 07.01.16.
  */
package object api {

  import upickle.default._

  val parseCreatedCPU = read[CreateCPURes] _
  val writeCreateCPU = write[CreateCPUReq] _
  val writeExOperator = write[ExecuteOperatorReq] _
}
