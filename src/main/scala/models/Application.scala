package models

import java.nio.charset.Charset

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.util.ByteString
import io.circe
import io.circe.{Decoder, HCursor, parser}
import io.circe.generic.auto._

case class Endpoint(versionId: String,
                    directVersionPublish: Boolean,
                    endpointUrl: String,
                    isStaging: Boolean,
                    assignedEndpointKey: Option[String],
                    region: Option[String],
                    endpointRegion: String,
                    publishedDateTime: String,
                    failedRegions: Option[String])

case class ApplicationEndpoints(production: Option[Endpoint],
                                staging: Option[Endpoint])

case class Application(id: String,
                       name: String,
                       description: String,
                       culture: String,
                       usageScenario: String,
                       domain: String,
                       versionsCount: Int,
                       createdDateTime: String,
                       endpointHitsCount: Int,
                       activeVersion: String,
                       ownerEmail: String,
                       tokenizerVersion: String,
                       endpoints: Option[ApplicationEndpoints])

object Application {
  implicit val unmarshaller
    : Unmarshaller[HttpEntity, Either[circe.Error, List[Application]]] = {
    implicit val applicationEndpointsDecoder: Decoder[ApplicationEndpoints] =
      Decoder.forProduct2("PRODUCTION", "STAGING")(ApplicationEndpoints.apply)


    implicit val decoder: Decoder[Application] = (c: HCursor) => {
      for {
        id <- c.downField("id").as[String]
        name <- c.downField("name").as[String]
        description <- c.downField("description").as[String]
        culture <- c.downField("culture").as[String]
        usageScenario <- c.downField("usageScenario").as[String]
        domain <- c.downField("domain").as[String]
        versionsCount <- c.downField("versionsCount").as[Int]
        createdDateTime <- c.downField("createdDateTime").as[String]
        endpointHitsCount <- c.downField("endpointHitsCount").as[Int]
        activeVersion <- c.downField("activeVersion").as[String]
        ownerEmail <- c.downField("ownerEmail").as[String]
        tokenizerVersion <- c.downField("tokenizerVersion").as[String]
        endpoints <- c.downField("endpoints").as[Option[ApplicationEndpoints]]
      } yield
        new Application(
          id,
          name,
          description,
          culture,
          usageScenario,
          domain,
          versionsCount,
          createdDateTime,
          endpointHitsCount,
          activeVersion,
          ownerEmail,
          tokenizerVersion,
          endpoints
        )
    }

    Unmarshaller.byteStringUnmarshaller.map {
      case ByteString.empty ⇒ throw Unmarshaller.NoContentException
      case data ⇒
        parser.decode[List[Application]](
          data.decodeString(Charset.forName("UTF-8"))
        )
    }
  }
}
