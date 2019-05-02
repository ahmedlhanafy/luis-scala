package graphql

import akka.http.scaladsl.model.HttpHeader
import repos.{ApplicationRepo, ModelRepo, UtteranceRepo}

case class MyContext(authHeader: HttpHeader,
                     applicationId: Option[String] = None,
                     versionId: Option[String] = None)(
  implicit val applicationRepo: ApplicationRepo,
  implicit val modelRepo: ModelRepo,
  implicit val utteranceRepo: UtteranceRepo
)
