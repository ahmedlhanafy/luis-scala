package graphql.mutations

import graphql.MyContext
import graphql.types.Model.{IntentType, EntityType}
import graphql.utils.updateCtxWithAppMetaData
import sangria.schema.{
  Argument,
  Field,
  ListInputType,
  ListType,
  ObjectType,
  OptionType,
  StringType,
  UpdateCtx,
  fields
}

object ModelMutations {
  val IdsArg = Argument("ids", ListInputType(StringType))
  val NameArg = Argument("name", StringType)
  val IdArg = Argument("id", StringType)
  val ApplicationIdArg = Argument("applicationId", StringType)
  val VersionIdArg = Argument("versionId", StringType)

  val IntentMutationsType = ObjectType(
    "Mutation",
    fields[MyContext, Unit](
      Field(
        "createIntent",
        OptionType(IntentType),
        arguments = ApplicationIdArg :: VersionIdArg :: NameArg :: Nil,
        resolve = c =>
          UpdateCtx(
            c.ctx.modelRepo.createIntent(
              c.arg(ApplicationIdArg),
              c.arg(VersionIdArg),
              c.arg(NameArg)
            )(c.ctx.authHeader)
          )(
            _ =>
              updateCtxWithAppMetaData(
                c,
                c.arg(ApplicationIdArg),
                c.arg(VersionIdArg)
            )
        )
      ),
      Field(
        "renameEntity",
        OptionType(EntityType),
        arguments = ApplicationIdArg :: VersionIdArg :: IdArg :: NameArg :: Nil,
        resolve = c =>
          UpdateCtx(
            c.ctx.modelRepo.renameEntity(
              c.arg(ApplicationIdArg),
              c.arg(VersionIdArg),
              c.arg(IdArg),
              c.arg(NameArg)
            )(c.ctx.authHeader)
          )(
            _ =>
              updateCtxWithAppMetaData(
                c,
                c.arg(ApplicationIdArg),
                c.arg(VersionIdArg)
            )
        )
      ),
      Field(
        "deleteIntents",
        ListType(StringType),
        arguments = ApplicationIdArg :: VersionIdArg :: IdsArg :: Nil,
        resolve = c =>
          c.ctx.modelRepo
            .deleteIntents(
              c.arg(ApplicationIdArg),
              c.arg(VersionIdArg),
              c.arg(IdsArg)
            )(c.ctx.authHeader)
      )
    )
  )
}
