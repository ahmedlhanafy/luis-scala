package repos

import akka.http.scaladsl.model.HttpHeader
import models.Application
import constants.baseUrl
import services.HttpService
import graphql.types.Pagination.PaginationArgs

import scala.concurrent.Future

class ApplicationRepo(implicit httpService: HttpService) {
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
