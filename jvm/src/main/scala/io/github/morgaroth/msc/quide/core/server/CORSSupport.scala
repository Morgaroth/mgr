package io.github.morgaroth.msc.quide.core.server

import spray.http.HttpHeaders._
import spray.http._
import spray.routing._

// see also https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS
trait CORSSupport {
  this: HttpService =>

  private val allowOriginHeader = `Access-Control-Allow-Origin`(AllOrigins)
  private val optionsCorsHeaders = List(
    `Access-Control-Allow-Headers`("Origin, X-Requested-With, Content-Type, Accept, Accept-Encoding, Accept-Language, Host, Referer, User-Agent", "X-User-Id", "x-user-id"),
    `Access-Control-Max-Age`(1728000)
  )

  def cors[T]: Directive0 = mapRequestContext { ctx =>
    ctx.withRouteResponseHandling {
      case Rejected(x) if ctx.request.method.equals(HttpMethods.OPTIONS) =>
        val allowedMethods: List[HttpMethod] = x.collect { case rejection: MethodRejection => rejection.supported } ::: List(HttpMethods.DELETE)
        ctx.complete {
          HttpResponse().withHeaders(
            `Access-Control-Allow-Methods`(HttpMethods.OPTIONS, allowedMethods: _*) :: allowOriginHeader ::
              optionsCorsHeaders
          )
        }
    }.withHttpResponseHeadersMapped { headers =>
      allowOriginHeader :: headers
    }
  }

  override def timeoutRoute = complete {
    HttpResponse(
      StatusCodes.InternalServerError,
      HttpEntity(ContentTypes.`text/plain(UTF-8)`, "The server was not able to produce a timely response to your request."),
      List(allowOriginHeader)
    )
  }
}