package graphql.types

import sangria.schema.{Argument, IntType}

object Pagination {
  case class PaginationArgs(skip: Int, take: Int)
  val paginationArgs =
    List(Argument("skip", IntType), Argument("take", IntType))
}
