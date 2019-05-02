package repos

import akka.http.scaladsl.model.HttpHeader
import constants.webApiBaseUrl
import graphql.types.Pagination.PaginationArgs
import models.{Utterance, UtterancePrediction}
import services.HttpService

import scala.concurrent.{ExecutionContext, Future}

class UtteranceRepo(implicit httpService: HttpService,
                    implicit val executionContext: ExecutionContext) {
  def getForModel(
    applicationId: String,
    versionId: String,
    modelId: String,
    pagination: PaginationArgs,
    withPredictions: Boolean
  )(implicit authHeader: HttpHeader): Future[List[Utterance]] = {
    val labelsFuture = httpService.get[List[Utterance]](
      s"$webApiBaseUrl/apps/$applicationId/versions/$versionId/models/$modelId/reviewLabels?skip=${pagination.skip}&take=${pagination.take}",
    )

    val predictionsFuture = httpService.get[List[UtterancePrediction]](
      s"$webApiBaseUrl/apps/$applicationId/versions/$versionId/models/$modelId/reviewPredictions?skip=${pagination.skip}&take=${pagination.take}",
    )

    if (withPredictions) {
      for {
        labels <- labelsFuture
        predictions <- predictionsFuture
      } yield
        labels zip predictions map {
          case (a, b) =>
            a.copy(
              alteredText = b.alteredText,
              intentPredictions = Some(b.intentPredictions),
              entityPredictions = b.entityPredictions
            )
        }
    } else
      labelsFuture
  }

  def predict(applicationId: String, versionId: String, text: Seq[String])(
    implicit authHeader: HttpHeader
  ): Future[List[UtterancePrediction]] = {
    httpService.post[Seq[String], List[UtterancePrediction]](
      uri =
        s"$webApiBaseUrl/apps/$applicationId/versions/$versionId/predict?getExampleIds=true",
      data = text
    )
  }
}
