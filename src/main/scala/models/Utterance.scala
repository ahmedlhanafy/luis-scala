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

case class Label(startTokenIndex: Int,
                 endTokenIndex: Int,
                 entity: SimpleEntity,
                 role: Option[EntityRole])

case class IntentPrediction(score: Int)

object Utterance {
  implicit val decoder: Decoder[Label] = (c: HCursor) => {
    for {
      id <- c.downField("id").as[String]
      startTokenIndex <- c.downField("startTokenIndex").as[Int]
      endTokenIndex <- c.downField("endTokenIndex").as[Int]
      entityName <- c.downField("entityName").as[String]
      roleId <- c.downField("roleId").as[Option[String]]
      roleName <- c.downField("role").as[Option[String]]
    } yield {
      val role = (roleId, roleName) match {
        case (Some(id), Some(name)) => Some(EntityRole(id, name))
        case _                      => None
      }
      Label(
        startTokenIndex,
        endTokenIndex,
        SimpleEntity(id, entityName, List.empty),
        role
      )
    }
  }

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
