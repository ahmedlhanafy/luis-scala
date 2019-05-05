package models

import java.nio.charset.Charset
import scala.language.postfixOps
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.util.ByteString
import io.circe
import io.circe.{Decoder, HCursor, parser}

case class Utterance(id: Int,
                     text: String,
                     tokenizedText: List[String],
                     entityLabels: Option[List[Label]],
                     createdDateTime: String,
                     modifiedDateTime: String,
                     alteredText: Option[String],
                     intentPredictions: Option[List[IntentPrediction]],
                     entityPredictions: Option[List[Label]])

case class UtterancePrediction(id: Option[Int],
                               text: Option[String],
                               tokenizedText: Option[List[String]],
                               alteredText: Option[String],
                               intentPredictions: List[IntentPrediction],
                               entityPredictions: Option[List[Label]])

object UtterancePrediction {
  implicit val decoder: Decoder[UtterancePrediction] = (c: HCursor) =>
    for {
      id <- c.downField("id").as[Option[Int]]
      text <- c.downField("text").as[Option[String]]
      tokenizedText <- c.downField("tokenizedText").as[Option[List[String]]]
      alteredText <- c.downField("alteredText").as[Option[String]]
      intentPredictions <- c
        .downField("intentPredictions")
        .as[List[IntentPrediction]]
      entityPredictions <- c
        .downField("entityPredictions")
        .as[Option[List[Label]]]
    } yield
      UtterancePrediction(
        id,
        text,
        tokenizedText,
        alteredText,
        intentPredictions,
        entityPredictions,
    )

  implicit val listUnMarshaller
    : Unmarshaller[HttpEntity, Either[circe.Error, List[UtterancePrediction]]] =
    Unmarshaller.byteStringUnmarshaller.map {
      case ByteString.empty ⇒ throw Unmarshaller.NoContentException
      case data ⇒ {
        val x = data.decodeString(Charset.forName("UTF-8"))
        parser.decode[List[UtterancePrediction]](x)
      }
    }
}

case class Label(startTokenIndex: Int,
                 endTokenIndex: Int,
                 entity: SimpleEntity,
                 role: Option[EntityRole],
                 phrase: String,
)

object Label {
  def populateLabelPhrase(labels: List[Label],
                          tokenizedText: List[String]): List[Label] = {
    labels.map({ label =>
      label.copy(
        phrase = tokenizedText
          .slice(label.startTokenIndex, label.endTokenIndex + 1)
          .mkString(" ")
      )
    })
  }

  implicit val decoder: Decoder[Label] = (c: HCursor) => {
    for {
      id <- c.downField("id").as[String]
      startTokenIndex <- c.downField("startTokenIndex").as[Int]
      endTokenIndex <- c.downField("endTokenIndex").as[Int]
      entityName <- c.downField("entityName").as[String]
      roleId <- c.downField("roleId").as[Option[String]]
      roleName <- c.downField("role").as[Option[String]]
      phrase <- c.downField("phrase").as[Option[String]]
    } yield {
      val role = (roleId, roleName) match {
        case (Some(""), Some(""))     => None
        case (Some(rId), Some(rName)) => Some(EntityRole(rId, rName))
        case (_, _)                   => None
      }

      Label(
        startTokenIndex,
        endTokenIndex,
        SimpleEntity(id, entityName, List.empty),
        role,
        phrase orNull
      )
    }
  }
}

case class IntentPrediction(intent: Intent, score: Double)

object IntentPrediction {
  implicit val decoder: Decoder[IntentPrediction] = (c: HCursor) =>
    for {
      id <- c.downField("id").as[String]
      name <- c.downField("name").as[String]
      score <- c.downField("score").as[Double]
    } yield IntentPrediction(Intent(id, name), score)

}

object Utterance {
  implicit val decoder: Decoder[Utterance] = (c: HCursor) =>
    for {

      id <- c.downField("id").as[Int]
      text <- c.downField("text").as[String]
      tokenizedText <- c.downField("tokenizedText").as[List[String]]
      alteredText <- c.downField("alteredText").as[Option[String]]
      entityLabels <- c.downField("entityLabels").as[Option[List[Label]]]
      createdDateTime <- c.downField("createdDateTime").as[String]
      modifiedDateTime <- c.downField("modifiedDateTime").as[String]
      intentPredictions <- c
        .downField("intentPredictions")
        .as[Option[List[IntentPrediction]]]
      entityPredictions <- c
        .downField("entityPredictions")
        .as[Option[List[Label]]]
    } yield
      Utterance(
        id,
        text,
        tokenizedText,
        entityLabels map { Label.populateLabelPhrase(_, tokenizedText) },
        createdDateTime,
        modifiedDateTime,
        alteredText,
        intentPredictions,
        entityPredictions
    )

  implicit val listUnMarshaller
    : Unmarshaller[HttpEntity, Either[circe.Error, List[Utterance]]] =
    Unmarshaller.byteStringUnmarshaller.map {
      case ByteString.empty ⇒ throw Unmarshaller.NoContentException
      case data ⇒
        parser.decode[List[Utterance]](
          data.decodeString(Charset.forName("UTF-8"))
        )
    }
}
