package io.github.morgaroth.msc.quide.core.services

import io.github.morgaroth.msc.quide.http.{CreateCPURes, CreateCPUReq, ExecuteOperatorReq}
import io.github.morgaroth.msc.quide.model.{QbitValue, Complex}
import spray.json.{RootJsonFormat, DefaultJsonProtocol}

/**
  * Created by mateusz on 06.01.16.
  */
trait marshallers extends DefaultJsonProtocol {
  implicit lazy val complex: RootJsonFormat[Complex] = jsonFormat(Complex.apply, "re", "im")
  implicit lazy val qbitvalue: RootJsonFormat[QbitValue] = jsonFormat2(QbitValue.apply)
  implicit lazy val exectureoperatorreq: RootJsonFormat[ExecuteOperatorReq] = jsonFormat2(ExecuteOperatorReq.apply)
  implicit lazy val fsgercdszfs: RootJsonFormat[CreateCPUReq] = jsonFormat1(CreateCPUReq.apply)
  implicit lazy val fgdsgvfsdgfds: RootJsonFormat[CreateCPURes] = jsonFormat2(CreateCPURes.apply)
}
