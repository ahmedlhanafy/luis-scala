import akka.http.scaladsl.model.HttpHeader
import repos.{ApplicationRepo, ModelRepo}

class MyContext(val authHeader: HttpHeader)(
  implicit val applicationRepo: ApplicationRepo,
  implicit val modelRepo: ModelRepo
)
