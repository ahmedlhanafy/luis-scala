package graphql.types

import graphql.MyContext
import graphql.types.Utterance.UtteranceType
import models._
import sangria.ast.Selection
import sangria.macros.derive.{Interfaces, ReplaceField, deriveObjectType}
import sangria.schema.{
  Context,
  Field,
  InterfaceType,
  ListType,
  ObjectType,
  OptionType,
  StringType,
  fields
}

import scala.concurrent.Future

object Model {
  def utterancesResolver[T <: Model](
    c: Context[MyContext, T]
  ): Future[List[Utterance]] = {
    val requiresPredictions = c.astFields(0).selections.exists {
      selection: Selection =>
        selection
          .asInstanceOf[sangria.ast.Field]
          .name == "intentPredictions" ||
        selection
          .asInstanceOf[sangria.ast.Field]
          .name == "entityPredictions"
    }

    c.ctx.utteranceRepo.getForModel(
      c.ctx.applicationId.getOrElse(""),
      c.ctx.versionId.getOrElse(""),
      c.value.id,
      requiresPredictions
    )(c.ctx.authHeader)
  }

  implicit val SublistType: ObjectType[Unit, Sublist] =
    deriveObjectType[Unit, Sublist]()

  implicit val EntityRoleType: ObjectType[Unit, EntityRole] =
    deriveObjectType[Unit, EntityRole]()

  implicit val ChildEntityType: ObjectType[Unit, ChildEntity] =
    deriveObjectType[Unit, ChildEntity]()

  val ModelType = InterfaceType(
    "Model",
    "",
    fields[Unit, Model](
      Field("id", StringType, resolve = _.value.id),
      Field("name", StringType, resolve = _.value.name)
    )
  )

  implicit val EntityType: InterfaceType[MyContext, Entity] = InterfaceType(
    "Entity",
    "",
    () =>
      fields[MyContext, Entity](
        Field("id", StringType, resolve = _.value.id),
        Field("name", StringType, resolve = _.value.name),
        Field("roles", ListType(EntityRoleType), resolve = _.value.roles),
        Field(
          "utterances",
          ListType(UtteranceType),
          resolve = c => utterancesResolver(c)
        )
    )
  )

  implicit val SimpleEntityType: ObjectType[Unit, SimpleEntity] =
    deriveObjectType[Unit, SimpleEntity](Interfaces(ModelType, EntityType))

  val ListEntityType: ObjectType[Unit, ListEntity] =
    deriveObjectType[Unit, ListEntity](Interfaces(ModelType, EntityType))

  val CompositeEntityType: ObjectType[Unit, CompositeEntity] =
    deriveObjectType[Unit, CompositeEntity](Interfaces(ModelType, EntityType))

  val HierarchicalEntityType: ObjectType[Unit, HierarchicalEntity] =
    deriveObjectType[Unit, HierarchicalEntity](
      Interfaces(ModelType, EntityType)
    )

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
          resolve = c => utterancesResolver(c)
        )
      )
    )
}
