package repos

import java.nio.charset.Charset

import akka.http.scaladsl.model.{HttpEntity, HttpHeader}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import constants.{baseUrl, webApiBaseUrl}
import graphql.types.Pagination.PaginationArgs
import io.circe
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Encoder, parser}
import models.{Intent, IntentPrediction, Label, Utterance, UtterancePrediction}
import services.HttpService

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class UtteranceRepo(implicit httpService: HttpService,
                    implicit val modelRepo: ModelRepo,
                    implicit val executionContext: ExecutionContext) {

  case class EntityLabel(startCharIndex: Int,
                         endCharIndex: Int,
                         entityName: String,
                         role: Option[String])

  case class ExampleResponse(ExampleId: Int, UtteranceText: String)
  case class ExampleRequest(intentName: String,
                            text: String,
                            entityLabels: List[EntityLabel])

  def create(
      applicationId: String,
      versionId: String,
      intentId: String,
      text: String
  )(implicit authHeader: HttpHeader): Future[Utterance] = {

    implicit val exampleResponseEncoder: Encoder[ExampleResponse] =
      Encoder.forProduct2("ExampleId", "UtteranceText")(
        e => (e.ExampleId, e.UtteranceText)
      )

    implicit val stringUnMarshaller
      : Unmarshaller[HttpEntity, Either[circe.Error, ExampleResponse]] = {
      Unmarshaller.byteStringUnmarshaller.mapWithCharset { (data, charset) =>
        parser.decode[ExampleResponse](
          data.decodeString(Charset.forName("UTF-8"))
        )
      }
    }

    Future
      .sequence(
        predict(applicationId, versionId, text :: Nil) :: modelRepo
          .getIntent(applicationId, versionId, intentId) :: Nil
      )
      .map(
        list =>
          transformPredictions(
            list.head.asInstanceOf[List[UtterancePrediction]],
            text,
            list(1).asInstanceOf[Option[Intent]]
        )
      )
      .flatMap {
        tuple: Option[
          (List[String],
           List[EntityLabel],
           List[Label],
           List[IntentPrediction],
           Option[Intent])
        ] =>
          val entityLabels: List[EntityLabel] = tuple.map(_._2) getOrElse List
            .empty[EntityLabel]

          val intentName: String = tuple
            .map(_._5)
            .flatMap(_.map(_.name))
            .getOrElse("")
          httpService
            .post[ExampleRequest, ExampleResponse](
              s"$baseUrl/apps/$applicationId/versions/$versionId/example",
              ExampleRequest(intentName, text, entityLabels)
            )
            .map(
              res =>
                Utterance(
                  res.ExampleId,
                  res.UtteranceText,
                  tuple map { _._1 } getOrElse List.empty[String],
                  None,
                  "",
                  "",
                  None,
                  tuple map (_._4),
                  tuple map (_._3)
              )
            )
      }
  }

  private def transformPredictions(
      utterancePredictions: List[UtterancePrediction],
      utteranceText: String,
      intentName: Option[Intent]
  ): Option[
    (List[String],
     List[EntityLabel],
     List[Label],
     List[IntentPrediction],
     Option[Intent])
  ] = {
    utterancePredictions match {
      case prediction :: _ =>
        (
          prediction.tokenizedText,
          prediction.entityPredictions,
          prediction.intentPredictions
        ) match {
          case (
              Some(tokenizedText),
              Some(entityPredictions),
              intentPredictions
              ) =>
//            val entityLabels = entityPredictions
//              .map(label => {
//                val (startCharIndex, endCharIndex) = tokenToCharIndeces(
//                  (label.startTokenIndex, label.endTokenIndex),
//                  prediction.text.getOrElse(""),
//                  tokenizedText
//                )
//                EntityLabel(
//                  startCharIndex,
//                  endCharIndex,
//                  label.entity.name,
//                  label.role map { _.name }
//                )
//              })
            Some(
              tokenizedText,
              List.empty,
              entityPredictions,
              intentPredictions,
              intentName
            )
          case (_, _, _) => None
        }
    }
  }

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
