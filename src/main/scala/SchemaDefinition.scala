import sangria.macros.derive.ObjectTypeDescription
import sangria.macros.derive._
import sangria.schema.{
  Argument,
  Context,
  Field,
  InterfaceType,
  ListType,
  ObjectType,
  OptionType,
  Schema,
  StringType,
  UpdateCtx,
  fields
}
import models.{
  Application,
  ApplicationEndpoints,
  ChildEntity,
  CompositeEntity,
  Endpoint,
  Entity,
  EntityRole,
  HierarchicalEntity,
  Intent,
  IntentPrediction,
  Label,
  ListEntity,
  Model,
  SimpleEntity,
  Sublist,
  Utterance
}
import sangria.ast.Selection

import scala.concurrent.Future

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
        resolve = c =>
          c.ctx.applicationRepo.getApp(c.arg[String]("id"))(c.ctx.authHeader)
      )
    )
  )

  val schema = Schema(QueryType)
}

object ModelSchemaDef {
  implicit val SublistType = deriveObjectType[Unit, Sublist]()

  implicit val EntityRoleType =
    deriveObjectType[Unit, EntityRole]()

  implicit val ChildEntityType =
    deriveObjectType[Unit, ChildEntity]()

  val ModelType = InterfaceType(
    "Model",
    "",
    fields[Unit, Model](
      Field("id", StringType, resolve = _.value.id),
      Field("name", StringType, resolve = _.value.name)
    )
  )

  implicit val EntityType = InterfaceType(
    "Entity",
    "",
    fields[Unit, Entity](
      Field("id", StringType, resolve = _.value.id),
      Field("name", StringType, resolve = _.value.name),
      Field("roles", ListType(EntityRoleType), resolve = _.value.roles)
    )
  )

  implicit val SimpleEntityType =
    deriveObjectType[Unit, SimpleEntity](Interfaces(ModelType, EntityType))

  val ListEntityType =
    deriveObjectType[Unit, ListEntity](Interfaces(ModelType, EntityType))

  val CompositeEntityType =
    deriveObjectType[Unit, CompositeEntity](Interfaces(ModelType, EntityType))

  val HierarchicalEntityType =
    deriveObjectType[Unit, HierarchicalEntity](
      Interfaces(ModelType, EntityType)
    )

  implicit val UtteranceType: ObjectType[MyContext, Utterance] = deriveObjectType[MyContext, Utterance]()


  implicit val IntentType: ObjectType[MyContext, Intent] =
    ObjectType(
      "Intent",
      "TODO",
      fields[MyContext, Intent](
        Field("id", StringType, resolve = _.value.id),
        Field("name", StringType, resolve = _.value.name),
        Field(
          "utterances",
          ListType(UtteranceType),
          resolve = c => {
            val requiresPredictions = c.astFields(0).selections.exists {
              selection: Selection =>
                selection
                  .asInstanceOf[sangria.ast.Field]
                  .name == "intentPredictions" ||
                selection
                  .asInstanceOf[sangria.ast.Field]
                  .name == "entityPredictions"
            }

            c.ctx.modelRepo.getUtterances(
              c.ctx.applicationId.getOrElse(""),
              c.ctx.versionId.getOrElse(""),
              c.value.id,
              requiresPredictions
            )(c.ctx.authHeader)
          }
        )
      )
    )

  implicit val IntentPredictionType = deriveObjectType[Unit, IntentPrediction]()

  implicit val LabelType =
    deriveObjectType[MyContext, Label](
      ReplaceField(
        "entity",
        Field(
          "entity",
          OptionType(EntityType),
          resolve = c => {
            val restOfFields = c.astFields(0).selections.filter {
              selection: Selection =>
                selection.asInstanceOf[sangria.ast.Field].name != "id" &&
                selection.asInstanceOf[sangria.ast.Field].name != "name"
            }
            if (restOfFields.isEmpty)
              Some(c.value.entity)
            else
              c.ctx.modelRepo.getEntity(
                c.ctx.applicationId.getOrElse(""),
                c.ctx.versionId.getOrElse(""),
                c.value.entity.id
              )(c.ctx.authHeader)
          }
        )
      )
    )


}

object ApplicationSchemaDef {
  import ModelSchemaDef._

  def updateCtx(c: Context[MyContext, Application]) = {
    c.ctx.copy(
      applicationId = Some(c.value.id),
      versionId = Some(c.value.activeVersion)
    )(applicationRepo = c.ctx.applicationRepo, modelRepo = c.ctx.modelRepo)
  }

  implicit val EndpointType = deriveObjectType[Unit, Endpoint]()

  implicit val ApplicationEndpointsType =
    deriveObjectType[Unit, ApplicationEndpoints]()

  val ApplicationType =
    deriveObjectType[MyContext, Application](
      ObjectTypeDescription("Luis Application"),
      AddFields(
        Field(
          "intents",
          ListType(IntentType),
          resolve = (c: Context[MyContext, Application]) =>
            UpdateCtx(
              c.ctx.modelRepo
                .getIntents(c.value.id, c.value.activeVersion)(c.ctx.authHeader)
            )(_ => updateCtx(c))
        ),
        Field(
          "entities",
          ListType(EntityType),
          resolve = c =>
            UpdateCtx(
              c.ctx.modelRepo
                .getEntities(c.value.id, c.value.activeVersion)(
                  c.ctx.authHeader
                )
            )(_ => updateCtx(c)),
          possibleTypes = List(
            SimpleEntityType,
            ListEntityType,
            CompositeEntityType,
            HierarchicalEntityType
          )
        )
      )
    )
}
