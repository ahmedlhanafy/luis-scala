package repos

import akka.http.scaladsl.model.HttpHeader
import constants.baseUrl
import graphql.types.Pagination.PaginationArgs
import io.circe.syntax._

import models.Application
import services.HttpService

import scala.concurrent.{ExecutionContext, Future}
import utils.getSuccessfulFutures

class ApplicationRepo(implicit httpService: HttpService,
                      implicit val executionContext: ExecutionContext) {

  def create(name: String, description: Option[String], culture: String)(
    implicit authHeader: HttpHeader
  ): Future[Application] =
    httpService.post(
      s"$baseUrl/apps",
      Map(
        "name" -> name,
        "description" -> (description getOrElse ""),
        "culture" -> culture
      ).asJson.toString()
    ) flatMap { get(_) }

  def delete(
    ids: Seq[String]
  )(implicit authHeader: HttpHeader): Future[Seq[String]] = {
    getSuccessfulFutures(
      ids
        .map(id => httpService.delete(s"$baseUrl/apps/$id/").map(_ => id))
    )
  }

  def get(id: String)(implicit authHeader: HttpHeader): Future[Application] =
    httpService
      .get[Application](uri = s"$baseUrl/apps/$id")

  def getAll(
    pagination: PaginationArgs
  )(implicit authHeader: HttpHeader): Future[List[Application]] =
    httpService.get[List[Application]](
      uri = s"$baseUrl/apps?skip=${pagination.skip}&take=${pagination.take}"
    )
}
