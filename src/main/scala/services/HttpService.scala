package services

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpHeader, HttpRequest}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.ActorMaterializer
import io.circe

import scala.concurrent.{ExecutionContext, Future}

class HttpService(implicit actorSystem: ActorSystem,
                  implicit val context: ExecutionContext,
                  implicit val materializer: ActorMaterializer) {

  def get[T](uri: String, headers: List[HttpHeader])(
    implicit unmarshaller: Unmarshaller[akka.http.scaladsl.model.HttpResponse,
                                        Either[io.circe.Error, T]]
  ): Future[T] = {
    Http()
      .singleRequest(HttpRequest(uri = uri, headers = headers))
      .flatMap(Unmarshal(_).to[Either[circe.Error, T]])
      .map {
        case Right(model) => model
        case Left(e)      => throw new Exception(e)
      }
  }
}
