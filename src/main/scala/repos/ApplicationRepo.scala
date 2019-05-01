package repos

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpHeader, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import io.circe
import models.Application
import constants.baseUrl

import scala.concurrent.{ExecutionContext, Future}

class ApplicationRepo(implicit actorSystem: ActorSystem,
                      implicit val context: ExecutionContext,
                      implicit val materializer: ActorMaterializer) {

  def getApp(
    id: String
  )(implicit authHeader: HttpHeader): Future[Application] = {
    Http()
      .singleRequest(
        HttpRequest(uri = s"$baseUrl/apps/$id", headers = authHeader :: Nil)
      )
      .flatMap(Unmarshal(_).to[Either[circe.Error, Application]])
      .map {
        case Right(app) => app
        case Left(e)    => throw e
      }
  }

  def getApps()(implicit authHeader: HttpHeader): Future[List[Application]] = {
    Http()
      .singleRequest(
        HttpRequest(
          uri = s"$baseUrl/apps?skip=0&take=10",
          headers = authHeader :: Nil
        )
      )
      .flatMap(Unmarshal(_).to[Either[circe.Error, List[Application]]])
      .map {
        case Right(apps) => apps
        case Left(_)     => List.empty[Application]
      }
  }
}
