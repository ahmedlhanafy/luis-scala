import java.nio.charset.Charset

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import sangria.macros.derive.ObjectTypeDescription
import sangria.macros.derive._
import sangria.schema.{
  Field,
  ListType,
  ObjectType,
  OptionType,
  Schema,
  UnionType,
  fields
}

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.model.{HttpEntity, HttpRequest}
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import io.circe
import models.{
  Application,
  ApplicationEndpoints,
  Endpoint,
  Entity,
  EntityRole,
  ListEntity,
  Model,
  SimpleEntity,
  Sublist
}

import scala.concurrent.Future

class AppService(implicit actorSystem: ActorSystem,
                 implicit val context: ExecutionContext,
                 implicit val materializer: ActorMaterializer) {

  def getEntities: Future[List[Model]] = {
    Future.successful(
      List(
        SimpleEntity("hs", "dsa", List.empty[EntityRole]),
        ListEntity(
          "sdsa",
          "sdd",
          List.empty,
          List(Sublist("id", "bro", List("dss", "ds")))
        )
      )
    )
    Http()
      .singleRequest(
        HttpRequest(
          uri =
            "https://westus.api.cognitive.microsoft.com/luis/api/v2.0/apps/cc441b0f-9a80-4089-b67b-7e844648615c/versions/0.1/models?take=1000",
          headers = RawHeader(
            "Ocp-Apim-Subscription-Key",
            "c0f3cc704f2e4d348d52cfc7ccfee85b"
          ) :: Nil
        )
      )
      .flatMap(Unmarshal(_).to[Either[circe.Error, List[Model]]])
      .map {
        case Right(apps) => apps
        case Left(_)     => List.empty[Model]
      }
  }

  def getApps(): Future[List[Application]] = {
    Http()
      .singleRequest(
        HttpRequest(
          uri =
            "https://westus.api.cognitive.microsoft.com/luis/api/v2.0/apps?skip=0&take=10",
          headers = RawHeader(
            "Ocp-Apim-Subscription-Key",
            "c0f3cc704f2e4d348d52cfc7ccfee85b"
          ) :: Nil
        )
      )
      .flatMap(Unmarshal(_).to[Either[circe.Error, List[Application]]])
      .map {
        case Right(apps) => apps
        case Left(_)     => List.empty[Application]
      }
  }
}

object LuisSchemaDefinition {
  val EndpointType = deriveObjectType[Unit, Endpoint]()
//  ObjectType("Endpoint", "Application's Endpoint", fields[Unit, Unit](
//    Field("STAGING", OptionType, resolve = _.value.id))
  ObjectType(
    "endpoints",
    fields[Unit, ApplicationEndpoints](
//    Field("STAGING", OptionType(EndpointType), resolve = _.)
    )
  )
  val ApplicationType =
    deriveObjectType[Unit, Application](
      ObjectTypeDescription("Luis Application")
    )

  implicit val EntityRoleType =
    deriveObjectType[Unit, EntityRole](ObjectTypeDescription("Luis Entity"))

  implicit val SublistType =
    deriveObjectType[Unit, Sublist](ObjectTypeDescription("Luis Entity"))

  implicit val SimpleEntityType =
    deriveObjectType[Unit, SimpleEntity](ObjectTypeDescription("Luis Entity"))

  implicit val ListEntityType =
    deriveObjectType[Unit, ListEntity](ObjectTypeDescription("Luis Entity"))

  implicit val EntityType = UnionType(
    "Entity",
    Some("Ay kalan"),
    List(SimpleEntityType, ListEntityType)
  )

  val QueryType = ObjectType(
    "Query",
    fields[AppService, Unit](
      Field(
        "applications",
        ListType(ApplicationType),
        resolve = _.ctx.getApps()
      ),
      Field("entities", ListType(EntityType), resolve = _.ctx.getEntities)
    )
  )

  val schema = Schema(QueryType)
}
