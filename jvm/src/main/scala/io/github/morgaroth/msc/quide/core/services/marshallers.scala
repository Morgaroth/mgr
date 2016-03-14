package io.github.morgaroth.msc.quide.core.services

import io.github.morgaroth.msc.quide.http.{CreateCPUReq, CPU, ExecuteOperatorReq}
import io.github.morgaroth.msc.quide.model.{Complex, QValue}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

/**
  * Created by mateusz on 06.01.16.
  */
trait marshallers extends DefaultJsonProtocol {
  implicit lazy val complex: RootJsonFormat[Complex] = jsonFormat(Complex.apply, "re", "im")
  implicit lazy val exectureoperatorreq: RootJsonFormat[ExecuteOperatorReq] = jsonFormat2(ExecuteOperatorReq.apply)
  implicit lazy val fsgercdszfs: RootJsonFormat[CreateCPUReq] = jsonFormat1(CreateCPUReq.apply)
  implicit lazy val fgdsgvfsdgfds: RootJsonFormat[CPU] = jsonFormat2(CPU.apply)
}
