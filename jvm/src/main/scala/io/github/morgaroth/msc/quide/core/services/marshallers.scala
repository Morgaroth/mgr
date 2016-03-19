package io.github.morgaroth.msc.quide.core.services

import io.github.morgaroth.msc.quide.http.{CPU, CreateCPUReq, ExecuteOperatorReq}
import io.github.morgaroth.msc.quide.model.Complex
import io.github.morgaroth.msc.quide.model.operators.{Z, _}
import spray.json._

import scala.util.Try

/**
  * Created by mateusz on 06.01.16.
  */
trait marshallers extends DefaultJsonProtocol {
  implicit lazy val complex: RootJsonFormat[Complex] = jsonFormat(Complex.apply, "re", "im")
  implicit lazy val exectureoperatorreq: RootJsonFormat[ExecuteOperatorReq] = jsonFormat2(ExecuteOperatorReq.apply)
  implicit lazy val fsgercdszfs: RootJsonFormat[CreateCPUReq] = jsonFormat1(CreateCPUReq.apply)
  implicit lazy val fgdsgvfsdgfds: RootJsonFormat[CPU] = jsonFormat2(CPU.apply)

  implicit object SingleOperatorJsonFormat extends JsonFormat[SingleQbitOperator] {
    def write(c: SingleQbitOperator): JsString =
      JsString(c.toString)

    def read(value: JsValue): SingleQbitOperator = value match {
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

  implicit lazy val controlledOperatorJsonFormat: RootJsonFormat[ControlledGate] = jsonFormat3(ControlledGate)


  implicit object OperatorJsonFormat extends JsonFormat[Operator] {

    import spray.json._

    def write(value: Operator): JsValue = value match {
      case c: SingleQbitOperator => c.toJson
      case c: ControlledGate => c.toJson
    }

    def read(value: JsValue): Operator =
      Try(value.convertTo[SingleQbitOperator]).recoverWith {
        case e => Try(value.convertTo[ControlledGate])
      }.getOrElse(deserializationError(s"Cannot convert $value to operator"))
  }

}
