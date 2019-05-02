package graphql.types

import graphql.MyContext
import models.{Application, ApplicationEndpoints, Endpoint}
import sangria.macros.derive.{
  AddFields,
  ObjectTypeDescription,
  deriveObjectType
}
import sangria.schema.{
  Argument,
  Context,
  Field,
  ListInputType,
  ListType,
  ObjectType,
  OptionType,
  StringType,
  UpdateCtx
}
import graphql.types.Model.{
  CompositeEntityType,
  EntityType,
  HierarchicalEntityType,
  IntentType,
  ListEntityType,
  SimpleEntityType
}
import graphql.types.Utterance.UtterancePredictionType

object Application {
  val idArgument = Argument("id", StringType)

  def updateCtx(c: Context[MyContext, Application]): MyContext = {
    c.ctx.copy(
      applicationId = Some(c.value.id),
      versionId = Some(c.value.activeVersion)
    )(
      applicationRepo = c.ctx.applicationRepo,
      modelRepo = c.ctx.modelRepo,
      utteranceRepo = c.ctx.utteranceRepo
    )
  }

  implicit val EndpointType: ObjectType[Unit, Endpoint] =
    deriveObjectType[Unit, Endpoint]()

  implicit val ApplicationEndpointsType
    : ObjectType[Unit, ApplicationEndpoints] =
    deriveObjectType[Unit, ApplicationEndpoints]()


  val ApplicationType: ObjectType[MyContext, Application] =
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
        ),
        Field(
          "intent",
          OptionType(IntentType),
          arguments = List(idArgument),
          resolve = c =>
            UpdateCtx(
              c.ctx.modelRepo
                .getIntent(
                  c.value.id,
                  c.value.activeVersion,
                  c.arg[String]("id")
                )(c.ctx.authHeader)
            )(_ => updateCtx(c)),
          possibleTypes = List(
            SimpleEntityType,
            ListEntityType,
            CompositeEntityType,
            HierarchicalEntityType
          )
        ),
        Field(
          "entity",
          OptionType(EntityType),
          arguments = List(idArgument),
          resolve = c =>
            UpdateCtx(
              c.ctx.modelRepo
                .getEntity(
                  c.value.id,
                  c.value.activeVersion,
                  c.arg[String]("id")
                )(c.ctx.authHeader)
            )(_ => updateCtx(c)),
          possibleTypes = List(
            SimpleEntityType,
            ListEntityType,
            CompositeEntityType,
            HierarchicalEntityType
          )
        ),
        Field(
          "predictions",
          ListType(UtterancePredictionType),
          arguments = Argument("utterances", ListInputType(StringType)) :: Nil,
          resolve = c =>
            UpdateCtx(
              c.ctx.utteranceRepo
                .predict(
                  c.value.id,
                  c.value.activeVersion,
                  c.arg[Seq[String]]("utterances")
                )(c.ctx.authHeader)
            )(_ => updateCtx(c))
        )
      )
    )
}
