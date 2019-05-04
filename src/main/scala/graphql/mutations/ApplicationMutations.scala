package graphql.mutations

import graphql.MyContext
import graphql.types.Application.{ApplicationType, CultureEnumType}
import sangria.schema.{
  Argument,
  Field,
  ListInputType,
  ListType,
  ObjectType,
  OptionInputType,
  StringType,
  fields
}

object ApplicationMutations {
  val IdsArg = Argument("ids", ListInputType(StringType))
  val NameArg = Argument("name", StringType)
  val DescriptionArg = Argument("description", OptionInputType(StringType))
  val CultureArg = Argument("culture", CultureEnumType)

  val ApplicationMutationsType = ObjectType(
    "Mutation",
    fields[MyContext, Unit](
      Field(
        "createApplication",
        ApplicationType,
        arguments = NameArg :: DescriptionArg :: CultureArg :: Nil,
        resolve = c =>
          c.ctx.applicationRepo.create(
            c.arg(NameArg),
            c.arg(DescriptionArg),
            c.arg(CultureArg).toString
          )(c.ctx.authHeader)
      ),
      Field(
        "deleteApplications",
        ListType(StringType),
        arguments = IdsArg :: Nil,
        resolve = c =>
          c.ctx.applicationRepo
            .delete(c.arg(IdsArg))(c.ctx.authHeader)
      )
    )
  )
}
