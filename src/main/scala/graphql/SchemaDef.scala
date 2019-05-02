package graphql

import sangria.schema.{
  Argument,
  Field,
  ListType,
  ObjectType,
  Schema,
  StringType,
  fields
}
import graphql.types.Application.ApplicationType
import graphql.types.Pagination.{PaginationArgs, paginationArgs}

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

  val schema = Schema(QueryType)
}
