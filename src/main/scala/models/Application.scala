package models

import java.nio.charset.Charset

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.util.ByteString
import io.circe
import io.circe.parser
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
                       tokenizerVersion: String)
//                       endpoints: Option[ApplicationEndpoints])

object Application {
  implicit val unmarshaller
    : Unmarshaller[HttpEntity, Either[circe.Error, List[Application]]] =
    Unmarshaller.byteStringUnmarshaller.map {
      case ByteString.empty ⇒ throw Unmarshaller.NoContentException
      case data ⇒
        parser.decode[List[Application]](
          data.decodeString(Charset.forName("UTF-8"))
        )
    }
}
