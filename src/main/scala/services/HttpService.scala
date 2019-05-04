package services

import java.nio.charset.Charset

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.ActorMaterializer
import io.circe
import io.circe.syntax._
import io.circe.{Encoder, Json, parser}
import sangria.execution.UserFacingError

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class UserException(message: String)
    extends Exception
    with UserFacingError {
  override def getMessage(): String = message
}

class HttpService(implicit actorSystem: ActorSystem,
                  implicit val context: ExecutionContext,
                  implicit val materializer: ActorMaterializer) {
  implicit val stringUnMarshaller
    : Unmarshaller[HttpEntity, Either[circe.Error, String]] = {
    Unmarshaller.byteStringUnmarshaller.mapWithCharset { (data, charset) =>
      parser.decode[String](data.decodeString(Charset.forName("UTF-8")))
    }
  }

  def get[T](uri: String)(
    implicit authHeader: HttpHeader,
    unMarshaller: Unmarshaller[HttpResponse, Either[io.circe.Error, T]]
  ): Future[T] =
    handleRes {
      Http().singleRequest(HttpRequest(uri = uri, headers = authHeader :: Nil))
    }

  def post[A, B](uri: String, data: A)(
    implicit authHeader: HttpHeader,
    unMarshaller: Unmarshaller[HttpResponse, Either[io.circe.Error, B]],
    decoder: Encoder[A]
  ): Future[B] =
    handleRes {
      Http()
        .singleRequest(
          HttpRequest(
            HttpMethods.POST,
            uri = uri,
            headers = authHeader :: Nil,
            entity = HttpEntity(
              ContentTypes.`application/json`,
              data.asJson.toString()
            )
          )
        )
    }

  def post(uri: String,
           data: String)(implicit authHeader: HttpHeader): Future[String] =
    handleRes[String] {
      Http()
        .singleRequest(
          HttpRequest(
            HttpMethods.POST,
            uri = uri,
            headers = authHeader :: Nil,
            entity = HttpEntity(ContentTypes.`application/json`, data)
          )
        )
    }

  def post[B](uri: String, data: Json)(
    implicit authHeader: HttpHeader,
    unMarshaller: Unmarshaller[HttpResponse, Either[io.circe.Error, B]],
  ): Future[B] =
    handleRes[B] {
      Http()
        .singleRequest(
          HttpRequest(
            HttpMethods.POST,
            uri = uri,
            headers = authHeader :: Nil,
            entity = HttpEntity(ContentTypes.`application/json`, data.toString())
          )
        )
    }

  def delete(uri: String)(implicit authHeader: HttpHeader): Future[Unit] = {
    Http()
      .singleRequest(
        HttpRequest(HttpMethods.DELETE, uri = uri, headers = authHeader :: Nil)
      )
      .map(Try(_))
      .flatMap {
        case Success(res) =>
          res.status.intValue() match {
            case 200 | 201 | 203 =>
              Future.successful()
            case _ =>
              Unmarshal(res.entity).to[String].flatMap { body =>
                Unmarshal(res.entity).to[String].flatMap { body =>
                  Future.failed(
                    UserException(
                      s"The response status is ${res.status} response body is $body"
                    )
                  )
                }
              }
          }
        case Failure(ex) =>
          Future.failed(ex)
      }
  }

  private def handleRes[T](future: Future[HttpResponse])(
    implicit unMarshaller: Unmarshaller[HttpResponse, Either[io.circe.Error, T]]
  ): Future[T] = {
    future
      .map(Try(_))
      .flatMap {
        case Success(res: HttpResponse) =>
          res.status.intValue() match {
            case 200 | 201 | 203 =>
              Unmarshal(res).to[Either[circe.Error, T]]
            case _ =>
              Unmarshal(res.entity).to[String].flatMap { body =>
                Unmarshal(res.entity).to[String].flatMap { body =>
                  Future.failed(
                    UserException(
                      s"The response status is ${res.status} response body is $body"
                    )
                  )
                }
              }
          }
        case Failure(ex) =>
          Future.failed(ex)
      }
      .flatMap {
        case Right(model) => Future.successful(model)
        case Left(e)      => Future.failed(e)
      }
  }
}
