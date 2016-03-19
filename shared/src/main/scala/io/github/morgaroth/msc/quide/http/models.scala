package io.github.morgaroth.msc.quide.http

import io.github.morgaroth.msc.quide.model.operators._

/**
  * Created by mateusz on 06.01.16.
  */
case class ExecuteOperatorReq(operator: Operator, index: Int)

case class CreateCPUReq(size: Int)

case class CPU(size: Int, id: String)