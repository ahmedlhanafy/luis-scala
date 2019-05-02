package services

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.ActorMaterializer
import io.circe
import io.circe.Encoder
import io.circe.syntax._

import scala.concurrent.{ExecutionContext, Future}

class HttpService(implicit actorSystem: ActorSystem,
                  implicit val context: ExecutionContext,
                  implicit val materializer: ActorMaterializer) {

  def get[T](uri: String)(
    implicit authHeader: HttpHeader,
    unMarshaller: Unmarshaller[HttpResponse, Either[io.circe.Error, T]]
  ): Future[T] =
    Http()
      .singleRequest(HttpRequest(uri = uri, headers = authHeader :: Nil))
      .flatMap(Unmarshal(_).to[Either[circe.Error, T]])
      .map {
        case Right(model) => model
        case Left(e)      => throw new Exception(e)
      }

  def post[A, B](uri: String, data: A)(
    implicit authHeader: HttpHeader,
    unMarshaller: Unmarshaller[HttpResponse, Either[io.circe.Error, B]],
    decoder: Encoder[A]
  ): Future[B] =
    Http()
      .singleRequest(
        HttpRequest(
          HttpMethods.POST,
          uri = uri,
          headers = authHeader :: Nil,
          entity =
            HttpEntity(ContentTypes.`application/json`, data.asJson.toString())
        )
      )
      .flatMap(Unmarshal(_).to[Either[circe.Error, B]])
      .map {
        case Right(model) => model
        case Left(e)      => throw new Exception(e)
      }

}
