package graphql

import sangria.schema.{
  Argument,
  Field,
  ListType,
  ObjectType,
  OptionInputType,
  Schema,
  StringType,
  fields
}
import graphql.types.Application.ApplicationType
import graphql.types.Pagination.{PaginationArgs, paginationArgs}
import graphql.mutations.ApplicationMutations.ApplicationMutationsType
import graphql.mutations.IntentMutations.IntentMutationsType
import graphql.mutations.UtteranceMutations.UtteranceMutationsType

object SchemaDef {
  val QueryType = ObjectType(
    "Query",
    fields[MyContext, Unit](
      Field(
        "applications",
        ListType(ApplicationType),
        arguments = paginationArgs,
        resolve = c =>
          c.ctx.applicationRepo.getAll(
            PaginationArgs(c.arg[Int]("skip"), c.arg[Int]("take"))
          )(c.ctx.authHeader)
      ),
      Field(
        "application",
        ApplicationType,
        arguments = List(Argument("id", StringType)),
        resolve =
          c => c.ctx.applicationRepo.get(c.arg[String]("id"))(c.ctx.authHeader)
      )
    )
  )

  val mutationFields
    : Vector[Field[MyContext, _]] = ApplicationMutationsType.fields ++ IntentMutationsType.fields ++ UtteranceMutationsType.fields

  val MutationsType = ObjectType(
    "Mutation",
    fields[MyContext, Unit](
      mutationFields
        .asInstanceOf[Seq[Field[MyContext, Unit]]]: _*
    )
  )

  val schema = Schema(QueryType, Some(MutationsType))
}
