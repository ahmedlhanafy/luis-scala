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
import graphql.types.Application.{ApplicationType}

object SchemaDef {
  val QueryType = ObjectType(
    "Query",
    fields[MyContext, Unit](
      Field(
        "applications",
        ListType(ApplicationType),
        resolve = c => c.ctx.applicationRepo.getAll()(c.ctx.authHeader)
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
