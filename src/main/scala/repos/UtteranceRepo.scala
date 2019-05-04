package repos

import java.nio.charset.Charset

import akka.http.scaladsl.model.{HttpEntity, HttpHeader}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import constants.{baseUrl, webApiBaseUrl}
import graphql.types.Pagination.PaginationArgs
import io.circe
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.circe.{Encoder, parser}
import models.{Label, Utterance, UtterancePrediction}
import services.HttpService

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class UtteranceRepo(implicit httpService: HttpService,
                    implicit val executionContext: ExecutionContext) {

  case class EntityLabel(startCharIndex: Int,
                         endCharIndex: Int,
                         entityName: String,
                         role: Option[String])

  case class ExampleResponse(ExampleId: Int, UtteranceText: String)

  def create(applicationId: String,
             versionId: String,
             intentId: String,
             intentName: String,
             text: String)(implicit authHeader: HttpHeader): Future[Int] = {

    implicit val exampleResponseEncoder: Encoder[ExampleResponse] =
      Encoder.forProduct2("ExampleId", "UtteranceText")(e => (e.ExampleId, e.UtteranceText))

    implicit val stringUnMarshaller
      : Unmarshaller[HttpEntity, Either[circe.Error, ExampleResponse]] = {
      Unmarshaller.byteStringUnmarshaller.mapWithCharset { (data, charset) =>
        parser.decode[ExampleResponse](
          data.decodeString(Charset.forName("UTF-8"))
        )
      }
    }

    predict(applicationId, versionId, text :: Nil)
      .map(transformPredictions(_, text))
      .flatMap {
        tuple: Option[(List[String], List[EntityLabel], List[Label])] =>
          val entityLabels: List[EntityLabel] = tuple.map(_._2) getOrElse List
            .empty[EntityLabel]

          val entityLabelsEncoder: Encoder[List[EntityLabel]] = deriveEncoder[List[EntityLabel]]
          httpService
            .post[ExampleResponse](
              s"$baseUrl/apps/$applicationId/versions/$versionId/example",
              Map(
                "intentName" -> intentName,
                "text" -> text,
//                "entityLabels" -> entityLabels.asJson(entityLabelsEncoder)
              ).asJson
            )
            .map(_.ExampleId)
      }
  }

  private def transformPredictions(
    utterancePredictions: List[UtterancePrediction],
    utteranceText: String
  ): Option[(List[String], List[EntityLabel], List[Label])] = {
    utterancePredictions match {
      case prediction :: _ =>
        (prediction.tokenizedText, prediction.entityPredictions) match {
          case (Some(tokenizedText), Some(entityPredictions)) =>
            val entityLabels = entityPredictions
              .map(label => {
                val (startCharIndex, endCharIndex) = tokenToCharIndeces(
                  (label.startTokenIndex, label.endTokenIndex),
                  utteranceText,
                  tokenizedText
                )
                EntityLabel(
                  startCharIndex,
                  endCharIndex,
                  label.entity.name,
                  label.role map { _.name }
                )
              })
            Some(tokenizedText, entityLabels, entityPredictions)
          case (_, _) => None
        }
    }
  }

//  private def nameMeLater(input: Option[(List[String], List[EntityLabel], List[Label])]): Unit = input match {
//    case Some((tokenizedText, entityLabels, entityPredictions)) =>
//  }

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

  private def tokenToCharIndeces(indices: (Int, Int),
                                 text: String,
                                 tokenizedText: List[String]): (Int, Int) = {
    val tokenCharacterMap: mutable.Map[Int, Int] =
      getCharIndexForTokens(text, tokenizedText)
    if (tokenCharacterMap
          .get(indices._1)
          .isDefined && tokenCharacterMap.get(indices._1).isDefined) {
      (
        tokenCharacterMap.getOrElse(indices._1, 0),
        tokenCharacterMap.getOrElse(indices._2, 0) + tokenizedText(indices._2).length - 1
      )
    }
    (0, 0)
  }

  private def getCharIndexForTokens(
    text: String,
    tokenizedText: List[String]
  ): scala.collection.mutable.Map[Int, Int] = {
    val output = scala.collection.mutable.Map[Int, Int]()
    var index = 0
    tokenizedText.foldLeft(0)((lastIndex: Int, t: String) => {
      output.put(index, text.indexOf(t, lastIndex))
      index += 1
      lastIndex + t.length + (if (text.charAt(lastIndex + t.length) == ' ') 1
                              else 0)
    })

    output
  }

}
