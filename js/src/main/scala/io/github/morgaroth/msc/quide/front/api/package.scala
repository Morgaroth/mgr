package io.github.morgaroth.msc.quide.front

import io.github.morgaroth.msc.quide.http.{CPU, CreateCPUReq, ExecuteOperatorReq}
import io.github.morgaroth.msc.quide.model.gates.{X, Y, Z, _}
import upickle.Js
import upickle.Js.Value

import scala.util.{Failure, Try}

/**
  * Created by mateusz on 07.01.16.
  */
package object api {

  import upickle.default._

  val parseCreatedCPU = read[CPU] _
  val writeCreateCPU = write[CreateCPUReq](_: CreateCPUReq, 0)
  implicit val singlereaderfromjson: Reader[SingleQbitGate] = Reader[SingleQbitGate] {
    case Js.Str(x) => x.toLowerCase() match {
      case "h" | "hadammard" => H
      case "i" | "identity" => I
      case "x" | "paulix" => X
      case "y" | "pauliy" => Y
      case "z" | "pauliz" => Z
    }
  }

  implicit val singlewritertojson: Writer[SingleQbitGate] = Writer[SingleQbitGate] {
    o => Js.Str(o.toString)
  }

  val singleReader: (Value) => SingleQbitGate = readJs[SingleQbitGate] _
  val singleWriter = writeJs[SingleQbitGate] _
  val controlledReader = readJs[ControlledGate] _
  val controlledWriter = writeJs[ControlledGate] _
  val multiControlledReader = readJs[MultiControlledGate] _
  val multiControlledWriter = writeJs[MultiControlledGate] _
  implicit val thing2Writer: Writer[Gate] = upickle.default.Writer[Gate] {
    case t: SingleQbitGate => singleWriter(t)
    case t: ControlledGate => controlledWriter(t)
    case t: MultiControlledGate => multiControlledWriter(t)
  }


  val possibleGateDeserializers = List(singleReader, controlledReader, multiControlledReader)

  implicit val thing2Reader: Reader[Gate] = upickle.default.Reader[Gate] {
    case str: Js.Value =>
      possibleGateDeserializers.foldLeft[Try[Gate]](Failure(new Exception("try not tried"))) {
        case (acc, reader) => acc.recoverWith { case _ => Try(reader(str)) }
      }.getOrElse(throw new RuntimeException(s"cannot read $str"))
  }

  val writeExOperator = write[ExecuteOperatorReq](_: ExecuteOperatorReq, 0)
}
