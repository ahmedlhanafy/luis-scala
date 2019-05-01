import sangria.macros.derive.ObjectTypeDescription
import sangria.macros.derive._
import sangria.schema.{
  Argument,
  Field,
  InterfaceType,
  ListType,
  ObjectType,
  Schema,
  StringType,
  fields
}
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
import repos.{ApplicationRepo, ModelRepo}

object SchemaDefinition {
  val QueryType = ObjectType(
    "Query",
    fields[MyContext, Unit](
      Field(
        "applications",
        ListType(ApplicationSchemaDef.ApplicationType),
        resolve = c => c.ctx.applicationRepo.getApps()(c.ctx.authHeader)
      ),
      Field(
        "application",
        ApplicationSchemaDef.ApplicationType,
        arguments = List(Argument("id", StringType)),
        resolve = c => c.ctx.applicationRepo.getApp(c.arg[String]("id"))(c.ctx.authHeader)
      )
    )
  )

  val schema = Schema(QueryType)
}

object ModelSchemaDef {
  implicit val SublistType = deriveObjectType[Unit, Sublist]()

  implicit val EntityRoleType =
    deriveObjectType[Unit, EntityRole]()

  val ModelType = InterfaceType(
    "Model",
    "",
    fields[Unit, Model](
      Field("id", StringType, resolve = _.value.id),
      Field("name", StringType, resolve = _.value.name)
    )
  )

  val IntentType =
    deriveObjectType[Unit, Intent](Interfaces(ModelType))

  val EntityType = InterfaceType(
    "Entity",
    "",
    fields[Unit, Entity](
      Field("id", StringType, resolve = _.value.id),
      Field("name", StringType, resolve = _.value.name),
      Field("roles", ListType(EntityRoleType), resolve = _.value.roles)
    )
  )

  val SimpleEntityType =
    deriveObjectType[Unit, SimpleEntity](Interfaces(ModelType, EntityType))

  val ListEntityType =
    deriveObjectType[Unit, ListEntity](Interfaces(ModelType, EntityType))

}

object ApplicationSchemaDef {
  implicit val EndpointType = deriveObjectType[Unit, Endpoint]()

  implicit val ApplicationEndpointsType =
    deriveObjectType[Unit, ApplicationEndpoints]()

  val ApplicationType =
    deriveObjectType[MyContext, Application](
      ObjectTypeDescription("Luis Application"),
      AddFields(
        Field(
          "intents",
          ListType(ModelSchemaDef.IntentType),
          resolve = c =>
            c.ctx.modelRepo.getIntents(c.value.id, c.value.activeVersion)(c.ctx.authHeader)
        ),
        Field(
          "entities",
          ListType(ModelSchemaDef.EntityType),
          resolve = c =>
            c.ctx.modelRepo.getEntities(c.value.id, c.value.activeVersion)(c.ctx.authHeader),
          possibleTypes =
            List(ModelSchemaDef.SimpleEntityType, ModelSchemaDef.ListEntityType)
        )
      )
    )
}
