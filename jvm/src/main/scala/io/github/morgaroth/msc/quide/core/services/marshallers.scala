package io.github.morgaroth.msc.quide.core.services

import io.github.morgaroth.msc.quide.http.{CPU, CreateCPUReq, ExecuteOperatorReq}
import io.github.morgaroth.msc.quide.model.Complex
import io.github.morgaroth.msc.quide.model.gates.{Z, _}
import spray.json._

import scala.util.{Failure, Try}

/**
  * Created by mateusz on 06.01.16.
  */
trait marshallers extends DefaultJsonProtocol {
  implicit lazy val complex: RootJsonFormat[Complex] = jsonFormat(Complex.apply, "re", "im")
  implicit lazy val exectureoperatorreq: RootJsonFormat[ExecuteOperatorReq] = jsonFormat2(ExecuteOperatorReq.apply)
  implicit lazy val fsgercdszfs: RootJsonFormat[CreateCPUReq] = jsonFormat1(CreateCPUReq.apply)
  implicit lazy val cpuJsonFormat: RootJsonFormat[CPU] = jsonFormat2(CPU.apply)

  implicit object SingleGateJsonFormat extends JsonFormat[SingleQbitGate] {
    def write(c: SingleQbitGate): JsString =
      JsString(c.toString)

    def read(value: JsValue): SingleQbitGate = value match {
      case JsString(name) => name.toLowerCase() match {
        case "h" | "hadammard" => H
        case "i" | "identity" => I
        case "x" | "paulix" => X
        case "y" | "pauliy" => Y
        case "z" | "pauliz" => Z
      }
      case v => deserializationError(s"Operator expected, found $v")
    }
  }

  implicit lazy val controlledGateJsonFormat: RootJsonFormat[ControlledGate] = jsonFormat2(ControlledGate)
  implicit lazy val multiControlledGateJsonFormat: RootJsonFormat[MultiControlledGate] = jsonFormat2(MultiControlledGate)


  implicit object OperatorJsonFormat extends JsonFormat[Gate] {

    import spray.json._

    def write(value: Gate): JsValue = value match {
      case c: SingleQbitGate => c.toJson
      case c: ControlledGate => c.toJson
      case c: MultiControlledGate => c.toJson
    }


    val possibleGateDeserializers: List[(JsValue) => Gate] = List(
      _.convertTo[SingleQbitGate],
      _.convertTo[ControlledGate],
      _.convertTo[MultiControlledGate]
    )

    def read(value: JsValue): Gate =
      possibleGateDeserializers.foldLeft[Try[Gate]](Failure(new Exception("try not tried"))) {
        case (curr, acc) => curr.recoverWith {
          case _ => Try(acc(value))
        }
      }.getOrElse(deserializationError(s"Cannot convert $value to gate"))
  }

}
