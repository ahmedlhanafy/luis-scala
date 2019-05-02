package repos

import akka.http.scaladsl.model.HttpHeader
import models.Application
import constants.baseUrl
import services.HttpService

import scala.concurrent.Future

class ApplicationRepo(implicit httpService: HttpService) {
  def get(id: String)(implicit authHeader: HttpHeader): Future[Application] =
    httpService
      .get[Application](uri = s"$baseUrl/apps/$id", headers = authHeader :: Nil)

  def getAll()(implicit authHeader: HttpHeader): Future[List[Application]] =
    httpService.get[List[Application]](
      uri = s"$baseUrl/apps?skip=0&take=100",
      headers = authHeader :: Nil
    )
}
