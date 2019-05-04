package repos

import akka.http.scaladsl.model.HttpHeader
import constants.baseUrl
import models.{Entity, Intent, Model}
import services.HttpService
import io.circe.syntax._

import scala.concurrent.{ExecutionContext, Future}
import utils.getSuccessfulFutures

class ModelRepo(implicit httpService: HttpService,
                implicit val executionContext: ExecutionContext) {

  object ModelType extends Enumeration {
    val Intent, Entity = Value
  }

  def createIntent(applicationId: String, versionId: String, name: String)(
    implicit authHeader: HttpHeader
  ): Future[Option[Intent]] = {
    httpService.post(
      s"$baseUrl/apps/$applicationId/versions/$versionId/intents",
      Map("name" -> name).asJson.toString()
    ) flatMap { getIntent(applicationId, versionId, _) }
  }

  def deleteIntents(applicationId: String, versionId: String, ids: Seq[String])(
    implicit authHeader: HttpHeader
  ): Future[Seq[String]] =
    getSuccessfulFutures(
      ids
        .map(
          id =>
            httpService
              .delete(
                s"$baseUrl/apps/$applicationId/versions/$versionId/intents/$id/"
              )
              .map(_ => id)
        )
    )

  def get(
    applicationId: String,
    versionId: String,
    modelId: String,
    `type`: ModelType.Value = ModelType.Intent
  )(implicit authHeader: HttpHeader): Future[Option[Model]] =
    httpService
      .get[Option[Model]](
        uri =
          s"$baseUrl/apps/$applicationId/versions/$versionId/models/$modelId"
      )
      .map {
        case Some(model) =>
          if (`type` == ModelType.Intent) Some(model.asInstanceOf[Intent])
          else Some(model.asInstanceOf[Entity])
        case None => None
      }

  def getAll(
    applicationId: String,
    versionId: String,
    `type`: ModelType.Value = ModelType.Intent
  )(implicit authHeader: HttpHeader): Future[List[Model]] =
    httpService
      .get[List[Model]](
        uri =
          s"$baseUrl/apps/$applicationId/versions/$versionId/models?take=500"
      )
      .map { models =>
        models.filter(
          if (`type` == ModelType.Intent) _.isInstanceOf[Intent]
          else _.isInstanceOf[Entity]
        )
      }

  def getIntents(applicationId: String, versionId: String)(
    implicit authHeader: HttpHeader
  ): Future[List[Intent]] =
    getAll(applicationId, versionId).map(_.map(_.asInstanceOf[Intent]))

  def getEntities(applicationId: String, versionId: String)(
    implicit authHeader: HttpHeader
  ): Future[List[Entity]] =
    getAll(applicationId, versionId, ModelType.Entity)
      .map(_.map { list =>
        list.asInstanceOf[Entity]
      })

  def getIntent(applicationId: String, versionId: String, intentId: String)(
    implicit authHeader: HttpHeader
  ): Future[Option[Intent]] =
    get(applicationId, versionId, intentId).map(_.map(_.asInstanceOf[Intent]))

  def getEntity(applicationId: String, versionId: String, entityId: String)(
    implicit authHeader: HttpHeader
  ): Future[Option[Entity]] =
    get(applicationId, versionId, entityId, ModelType.Entity)
      .map(_.map(_.asInstanceOf[Entity]))

}
