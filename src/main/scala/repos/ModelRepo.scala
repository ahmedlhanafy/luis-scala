package repos

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpHeader, HttpRequest}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import io.circe
import models.{Entity, Intent, Model}
import constants.baseUrl

import scala.concurrent.{ExecutionContext, Future}



class ModelRepo(implicit actorSystem: ActorSystem,
                 implicit val context: ExecutionContext,
                 implicit val materializer: ActorMaterializer) {

  object ModelType extends Enumeration {
    val Intent, Entity = Value
  }

  def getIntents(applicationId: String,
                 versionId: String)(implicit authHeader: HttpHeader): Future[List[Intent]] =
    getModels(applicationId, versionId).map(_.map(_.asInstanceOf[Intent]))

  def getEntities(applicationId: String,
                  versionId: String)(implicit authHeader: HttpHeader): Future[List[Entity]] =
    getModels(applicationId, versionId, ModelType.Entity)
      .map(_.map { list =>
        list.asInstanceOf[Entity]
      })

  def getModels(applicationId: String,
                versionId: String,
                `type`: ModelType.Value = ModelType.Intent)(implicit authHeader: HttpHeader) =
    Http()
      .singleRequest(
        HttpRequest(
          uri =
            s"$baseUrl/apps/$applicationId/versions/$versionId/models?take=500",
          headers = authHeader :: Nil
        )
      )
      .flatMap(Unmarshal(_).to[Either[circe.Error, List[Model]]])
      .map {
        case Right(intents) =>
          intents.filter(
            if (`type` == ModelType.Intent) _.isInstanceOf[Intent]
            else _.isInstanceOf[Entity]
          )
        case Left(_) => List.empty[Model]
      }
}