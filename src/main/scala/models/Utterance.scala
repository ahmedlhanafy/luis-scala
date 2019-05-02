package models

import java.nio.charset.Charset

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.util.ByteString
import io.circe
import io.circe.{Decoder, HCursor, parser}
import io.circe.generic.auto._

case class Utterance(id: Int,
                     text: String,
                     tokenizedText: List[String],
                     entityLabels: Option[List[Label]],
                     createdDateTime: String,
                     modifiedDateTime: String,
                     alteredText: Option[String],
                     intentPredictions: Option[List[IntentPrediction]],
                     entityPredictions: Option[List[Label]])

case class UtterancePrediction(id: Int,
                               alteredText: Option[String],
                               intentPredictions: List[IntentPrediction],
                               entityPredictions: Option[List[Label]])

object UtterancePrediction {

  implicit val listUnmarshaller
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
                 role: Option[EntityRole])

object Label {
  implicit val decoder: Decoder[Label] = (c: HCursor) => {
    for {
      id <- c.downField("id").as[String]
      startTokenIndex <- c.downField("startTokenIndex").as[Int]
      endTokenIndex <- c.downField("endTokenIndex").as[Int]
      entityName <- c.downField("entityName").as[String]
      roleId <- c.downField("roleId").as[String]
      roleName <- c.downField("role").as[String]
    } yield {
      val role = (roleId, roleName) match {
        case ("", "")     => None
        case (rId, rName) => Some(EntityRole(rId, rName))
      }

      Label(
        startTokenIndex,
        endTokenIndex,
        SimpleEntity(id, entityName, List.empty),
        role
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
  implicit val listUnmarshaller
    : Unmarshaller[HttpEntity, Either[circe.Error, List[Utterance]]] =
    Unmarshaller.byteStringUnmarshaller.map {
      case ByteString.empty ⇒ throw Unmarshaller.NoContentException
      case data ⇒ {
        val x = data.decodeString(Charset.forName("UTF-8"))
        parser.decode[List[Utterance]](x)
      }
    }
}
