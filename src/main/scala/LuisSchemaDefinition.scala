import SchemaDefinition.Character
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import sangria.macros.derive.ObjectTypeDescription
import sangria.macros.derive._
import sangria.schema.{
  Field,
  InterfaceType,
  ListType,
  ObjectType,
  OptionType,
  ProjectionName,
  Schema,
  StringType,
  UnionType,
  fields,
  interfaces
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
  Intent,
  ListEntity,
  Model,
  SimpleEntity,
  Sublist
}

import scala.concurrent.Future

class AppService(implicit actorSystem: ActorSystem,
                 implicit val context: ExecutionContext,
                 implicit val materializer: ActorMaterializer) {
  val baseUrl = "https://westus.api.cognitive.microsoft.com/luis/api/v2.0"
  val authHeader =
    RawHeader("Ocp-Apim-Subscription-Key", "c0f3cc704f2e4d348d52cfc7ccfee85b")

  object ModelType extends Enumeration {
    val Intent, Entity = Value
  }

  def getIntents(applicationId: String,
                 versionId: String): Future[List[Intent]] =
    getModels(applicationId, versionId).map(_.map(_.asInstanceOf[Intent]))

  def getEntities(applicationId: String,
                  versionId: String): Future[List[Entity]] =
    getModels(applicationId, versionId, ModelType.Entity)
      .map(_.map { list =>
        list.asInstanceOf[Entity] })

  def getModels(applicationId: String,
                versionId: String,
                `type`: ModelType.Value = ModelType.Intent) =
    Http()
      .singleRequest(
        HttpRequest(
          uri =
            s"$baseUrl/apps/$applicationId/versions/$versionId/models?take=500",
          headers = authHeader :: Nil
        )
      )
      .flatMap(Unmarshal(_).to[Either[circe.Error, List[Model]]])
      .map {
        case Right(intents) =>
          intents.filter(
            if (`type` == ModelType.Intent) _.isInstanceOf[Intent]
            else _.isInstanceOf[Entity]
          )
        case Left(_) => List.empty[Model]
      }

  def getApps(): Future[List[Application]] = {
    Http()
      .singleRequest(
        HttpRequest(
          uri = s"$baseUrl/apps?skip=0&take=10",
          headers = authHeader :: Nil
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
  implicit val EndpointType = deriveObjectType[Unit, Endpoint]()

  implicit val ModelType = ObjectType(
    "Model",
    "",
    fields[Unit, Model](
      Field("id", StringType, resolve = _.value.id),
      Field("name", StringType, resolve = _.value.name)
    )
  )

  implicit val IntentType = deriveObjectType[Unit, Intent]()

  implicit val EntityRoleType = deriveObjectType[Unit, EntityRole]()

  implicit val EntityType = InterfaceType(
    "EntityModel",
    "",
    fields[Unit, Entity](
      Field("id", StringType, resolve = _.value.id),
      Field("name", StringType, resolve = _.value.name),
      Field("roles", ListType(EntityRoleType), resolve = _.value.roles)
    )
  )

  implicit val SimpleEntityType = deriveObjectType[Unit, SimpleEntity]()
  implicit val SublistType = deriveObjectType[Unit, Sublist]()
  implicit val ListEntityType = deriveObjectType[Unit, ListEntity]()

  implicit val ApplicationEndpointsType =
    deriveObjectType[Unit, ApplicationEndpoints]()

  val ApplicationType =
    deriveObjectType[AppService, Application](
      ObjectTypeDescription("Luis Application"),
      AddFields(
        Field(
          "intents",
          ListType(IntentType),
          resolve =
            ctx => ctx.ctx.getIntents(ctx.value.id, ctx.value.activeVersion)
        ),
        Field(
          "entities",
          ListType(
            EntityType
//            UnionType("Entity", types = List(SimpleEntityType, ListEntityType))
          ),
          resolve =
            ctx => ctx.ctx.getEntities(ctx.value.id, ctx.value.activeVersion)
        )
      )
    )

  val QueryType = ObjectType(
    "Query",
    fields[AppService, Unit](
      Field(
        "applications",
        ListType(ApplicationType),
        resolve = _.ctx.getApps()
      )
    )
  )

  val schema = Schema(QueryType)
}
