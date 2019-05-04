package graphql.types

import sangria.schema.{Argument, IntType, OptionInputType}

object Pagination {
  case class PaginationArgs(skip: Int = 0, take: Int)
  val paginationArgs =
    List(
      Argument("skip", OptionInputType(IntType), defaultValue = 0),
      Argument("take", IntType)
    )
}
