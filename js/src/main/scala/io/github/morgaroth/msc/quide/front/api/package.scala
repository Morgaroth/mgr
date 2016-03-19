package io.github.morgaroth.msc.quide.front

import io.github.morgaroth.msc.quide.http.{CPU, CreateCPUReq, ExecuteOperatorReq}
import io.github.morgaroth.msc.quide.model.operators.{X, Y, Z, _}
import upickle.Js
import upickle.Js.Value

import scala.util.Try

/**
  * Created by mateusz on 07.01.16.
  */
package object api {

  import upickle.default._

  val parseCreatedCPU = read[CPU] _
  val writeCreateCPU = write[CreateCPUReq](_: CreateCPUReq, 0)
  implicit val singlereaderfromjson: Reader[SingleQbitOperator] = Reader[SingleQbitOperator]{
    case Js.Str(x) => x.toLowerCase() match {
      case "h" | "hadammard" => H
      case "i" | "identity" => I
      case "x" | "paulix" => X
      case "y" | "pauliy" => Y
      case "z" | "pauliz" => Z
    }
  }

  implicit val singlewritertojson: Writer[SingleQbitOperator] = Writer[SingleQbitOperator]{
    o => Js.Str(o.toString)
  }

  val singleReader: (Value) => SingleQbitOperator = readJs[SingleQbitOperator] _
  val singleWriter = writeJs[SingleQbitOperator] _
  val controlledReader = readJs[ControlledGate] _
  val controlledWriter = writeJs[ControlledGate] _
  implicit val thing2Writer: Writer[Operator] = upickle.default.Writer[Operator] {
    case t: SingleQbitOperator => singleWriter(t)
    case t: ControlledGate => controlledWriter(t)
  }
  implicit val thing2Reader: Reader[Operator] = upickle.default.Reader[Operator] {
    case str: Js.Value => Try[Operator](singleReader(str)).recoverWith { case _ => Try(controlledReader(str))}
      .getOrElse(throw new RuntimeException(s"cannot read $str"))
  }

  val writeExOperator = write[ExecuteOperatorReq](_: ExecuteOperatorReq, 0)
}
