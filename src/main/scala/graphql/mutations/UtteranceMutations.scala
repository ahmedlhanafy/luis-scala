package graphql.mutations

import graphql.types.Utterance.UtteranceType
import graphql.MyContext
import graphql.utils.updateCtxWithAppMetaData
import sangria.schema.{
  Argument,
  Field,
  IntType,
  ObjectType,
  StringType,
  UpdateCtx,
  fields
}

object UtteranceMutations {
  val TextArg = Argument("text", StringType)
  val ApplicationIdArg = Argument("applicationId", StringType)
  val VersionIdArg = Argument("versionId", StringType)
  val IntentIdArg = Argument("intentId", StringType)

  val UtteranceMutationsType = ObjectType(
    "Mutation",
    fields[MyContext, Unit](
      Field(
        "createUtterance",
        UtteranceType,
        arguments = TextArg :: ApplicationIdArg :: VersionIdArg :: IntentIdArg :: Nil,
        resolve = c =>
          UpdateCtx(
            c.ctx.utteranceRepo.create(
              c.arg(ApplicationIdArg),
              c.arg(VersionIdArg),
              c.arg(IntentIdArg),
              c.arg(TextArg)
            )(c.ctx.authHeader))(
            _ =>
              updateCtxWithAppMetaData(
                c,
                c.arg(ApplicationIdArg),
                c.arg(VersionIdArg)
            )
        )
      )
    )
  )
}
