package io.github.morgaroth.quide.core.http

import io.github.morgaroth.quide.core.model.gates.Gate

/**
  * Created by mateusz on 06.01.16.
  */
case class ExecuteOperatorReq(gate: Gate, index: Int)

case class CreateCPUReq(size: Int, `type`: String)

case class CPU(size: Int, id: String)