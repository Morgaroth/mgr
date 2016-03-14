package io.github.morgaroth.msc.quide.http

/**
  * Created by mateusz on 06.01.16.
  */
case class ExecuteOperatorReq(operator: String, index: Int)

case class CreateCPUReq(size: Int)

case class CPU(size: Int, id: String)