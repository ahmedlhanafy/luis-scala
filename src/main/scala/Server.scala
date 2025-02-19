import GraphQLRequestUnmarshaller._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import constants.authHeaderKey
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport._
import graphql.{MyContext, SchemaDef}
import io.circe._
import io.circe.optics.JsonPath._
import io.circe.parser._
import repos.{ApplicationRepo, ModelRepo, UtteranceRepo}
import sangria.ast.Document
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.circe._
import sangria.parser.DeliveryScheme.Try
import sangria.parser.{QueryParser, SyntaxError}
import sangria.slowlog.SlowLog
import services.HttpService

import scala.util.control.NonFatal
import scala.util.{Failure, Success}

object Server extends App with CorsSupport {
  import system.dispatcher
  implicit val system = ActorSystem("sangria-server")
  implicit val materializer = ActorMaterializer()
  implicit val httpService = new HttpService()
  implicit val applicationRepo = new ApplicationRepo()
  implicit val modelRepo = new ModelRepo()
  implicit val utteranceRepo = new UtteranceRepo()

  def executeGraphQL(query: Document,
                     operationName: Option[String],
                     variables: Json,
                     tracing: Boolean,
                     headers: Seq[HttpHeader]) =
    complete(
      Executor
        .execute(
          SchemaDef.schema,
          query,
          MyContext(
            headers
              .find(_.name() == authHeaderKey)
              .getOrElse(RawHeader(authHeaderKey, ""))
          ),
          variables = if (variables.isNull) Json.obj() else variables,
          operationName = operationName,
          middleware =
            if (tracing) SlowLog.apolloTracing :: Nil
            else Nil,
        )
        .map(OK → _)
        .recover {
          case error: QueryAnalysisError ⇒ BadRequest → error.resolveError
          case error: ErrorWithResolver ⇒
            InternalServerError → error.resolveError
        }
    )

  def formatError(error: Throwable): Json = error match {
    case syntaxError: SyntaxError ⇒
      Json.obj(
        "errors" → Json.arr(
          Json.obj(
            "message" → Json.fromString(syntaxError.getMessage),
            "locations" → Json.arr(
              Json.obj(
                "line" → Json
                  .fromBigInt(syntaxError.originalError.position.line),
                "column" → Json
                  .fromBigInt(syntaxError.originalError.position.column)
              )
            )
          )
        )
      )
    case NonFatal(e) ⇒
      formatError(e.getMessage)
    case e ⇒
      throw e
  }

  def formatError(message: String): Json =
    Json.obj(
      "errors" → Json.arr(Json.obj("message" → Json.fromString(message)))
    )

  val route: Route =
    optionalHeaderValueByName("X-Apollo-Tracing") { tracing ⇒
      path("graphql") {
        extractRequest { req =>
          get {
            explicitlyAccepts(`text/html`) {
              getFromResource("assets/playground.html")
            } ~
              parameters('query, 'operationName.?, 'variables.?) {
                (query, operationName, variables) ⇒
                  QueryParser.parse(query) match {
                    case Success(ast) ⇒
                      variables.map(parse) match {
                        case Some(Left(error)) ⇒
                          complete(BadRequest, formatError(error))
                        case Some(Right(json)) ⇒
                          executeGraphQL(
                            ast,
                            operationName,
                            json,
                            tracing.isDefined,
                            req.headers
                          )
                        case None ⇒
                          executeGraphQL(
                            ast,
                            operationName,
                            Json.obj(),
                            tracing.isDefined,
                            req.headers
                          )
                      }
                    case Failure(error) ⇒
                      complete(BadRequest, formatError(error))
                  }
              }
          } ~
            post {
              parameters('query.?, 'operationName.?, 'variables.?) {
                (queryParam, operationNameParam, variablesParam) ⇒
                  entity(as[Json]) { body ⇒
                    val query = queryParam orElse root.query.string
                      .getOption(body)
                    val operationName = operationNameParam orElse root.operationName.string
                      .getOption(body)
                    val variablesStr = variablesParam orElse root.variables.string
                      .getOption(body)

                    query.map(QueryParser.parse(_)) match {
                      case Some(Success(ast)) ⇒
                        variablesStr.map(parse) match {
                          case Some(Left(error)) ⇒
                            complete(BadRequest, formatError(error))
                          case Some(Right(json)) ⇒
                            executeGraphQL(
                              ast,
                              operationName,
                              json,
                              tracing.isDefined,
                              req.headers
                            )
                          case None ⇒
                            executeGraphQL(
                              ast,
                              operationName,
                              root.variables.json.getOption(body) getOrElse Json
                                .obj(),
                              tracing.isDefined,
                              req.headers
                            )
                        }
                      case Some(Failure(error)) ⇒
                        complete(BadRequest, formatError(error))
                      case None ⇒
                        complete(BadRequest, formatError("No query to execute"))
                    }
                  } ~
                    entity(as[Document]) { document ⇒
                      variablesParam.map(parse) match {
                        case Some(Left(error)) ⇒
                          complete(BadRequest, formatError(error))
                        case Some(Right(json)) ⇒
                          executeGraphQL(
                            document,
                            operationNameParam,
                            json,
                            tracing.isDefined,
                            req.headers
                          )
                        case None ⇒
                          executeGraphQL(
                            document,
                            operationNameParam,
                            Json.obj(),
                            tracing.isDefined,
                            req.headers
                          )
                      }
                    }
              }
            }
        }
      }
    } ~
      (get & pathEndOrSingleSlash) {
        redirect("/graphql", PermanentRedirect)
      }

  Http().bindAndHandle(
    corsHandler(route),
    "0.0.0.0",
    sys.env.getOrElse("PORT", "8080").toInt
  )
}
