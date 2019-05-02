package repos

import akka.http.scaladsl.model.HttpHeader
import constants.webApibaseUrl
import models.{Utterance, UtterancePrediction}
import services.HttpService

import scala.concurrent.{ExecutionContext, Future}

class UtteranceRepo(implicit httpService: HttpService,
                    implicit val executionContext: ExecutionContext) {
  def getForModel(
    applicationId: String,
    versionId: String,
    modelId: String,
    withPredictions: Boolean
  )(implicit authHeader: HttpHeader): Future[List[Utterance]] = {
    val labelsFuture = httpService.get[List[Utterance]](
      uri =
        s"$webApibaseUrl/apps/$applicationId/versions/$versionId/models/$modelId/reviewLabels?skip=0&take=10",
      headers = authHeader :: Nil
    )

    val predictionsFuture = httpService.get[List[UtterancePrediction]](
      uri =
        s"$webApibaseUrl/apps/$applicationId/versions/$versionId/models/$modelId/reviewPredictions?skip=0&take=10",
      headers = authHeader :: Nil
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
}
