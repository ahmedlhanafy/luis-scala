package repos

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpHeader, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import io.circe
import models.{Entity, Intent, Model, Utterance}
import constants.{baseUrl, webApibaseUrl}

import scala.concurrent.{ExecutionContext, Future}

class ModelRepo(implicit actorSystem: ActorSystem,
                implicit val context: ExecutionContext,
                implicit val materializer: ActorMaterializer) {

  object ModelType extends Enumeration {
    val Intent, Entity = Value
  }

  def getUtterances(applicationId: String, versionId: String, modelId: String)(
    implicit authHeader: HttpHeader
  ): Future[List[Utterance]] = {
    Http()
      .singleRequest(
        HttpRequest(
          uri =
            s"$webApibaseUrl/apps/$applicationId/versions/$versionId/models/$modelId/reviewLabels?skip=0&take=10",
          headers = authHeader :: Nil
        )
      )
      .flatMap(Unmarshal(_).to[Either[circe.Error, List[Utterance]]])
      .map {
        case Right(utterances) => utterances
        case Left(e)           => throw e
      }
  }

  def getIntents(applicationId: String, versionId: String)(
    implicit authHeader: HttpHeader
  ): Future[List[Intent]] =
    getModels(applicationId, versionId).map(_.map(_.asInstanceOf[Intent]))

  def getEntities(applicationId: String, versionId: String)(
    implicit authHeader: HttpHeader
  ): Future[List[Entity]] =
    getModels(applicationId, versionId, ModelType.Entity)
      .map(_.map { list =>
        list.asInstanceOf[Entity]
      })

  def getIntent(applicationId: String, versionId: String, intentId: String)(
    implicit authHeader: HttpHeader
  ): Future[Option[Intent]] =
    getModel(applicationId, versionId, intentId).map(
      _.map(_.asInstanceOf[Intent])
    )

  def getEntity(applicationId: String, versionId: String, entityId: String)(
    implicit authHeader: HttpHeader
  ): Future[Option[Entity]] =
    getModel(applicationId, versionId, entityId, ModelType.Entity)
      .map(_.map(_.asInstanceOf[Entity]))

  def getModel(
    applicationId: String,
    versionId: String,
    modelId: String,
    `type`: ModelType.Value = ModelType.Intent
  )(implicit authHeader: HttpHeader): Future[Option[Model]] =
    Http()
      .singleRequest(
        HttpRequest(
          uri =
            s"$baseUrl/apps/$applicationId/versions/$versionId/models/$modelId",
          headers = authHeader :: Nil
        )
      )
      .flatMap(Unmarshal(_).to[Either[circe.Error, Model]])
      .map {
        case Right(model) =>
          if (`type` == ModelType.Intent) Some(model.asInstanceOf[Intent])
          else Some(model.asInstanceOf[Entity])
        case Left(_) => None
      }

  def getModels(
    applicationId: String,
    versionId: String,
    `type`: ModelType.Value = ModelType.Intent
  )(implicit authHeader: HttpHeader) =
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
