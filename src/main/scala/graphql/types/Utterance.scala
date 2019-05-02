package graphql.types

import graphql.MyContext
import graphql.types.Model._
import models.{IntentPrediction, Label, Utterance, UtterancePrediction}
import sangria.ast.Selection
import sangria.execution.deferred.{Fetcher, HasId}
import sangria.macros.derive.{ReplaceField, deriveObjectType}
import sangria.schema.{Field, ObjectType, OptionType}

object Utterance {
  implicit val IntentPredictionType: ObjectType[Unit, IntentPrediction] =
    deriveObjectType[Unit, IntentPrediction]()

  implicit val LabelType: ObjectType[MyContext, Label] =
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

  implicit val UtteranceType: ObjectType[Unit, Utterance] =
    deriveObjectType[Unit, Utterance]()

  implicit val UtterancePredictionType: ObjectType[Unit, UtterancePrediction] =
    deriveObjectType[Unit, UtterancePrediction]()
}
