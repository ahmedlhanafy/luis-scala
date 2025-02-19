//import org.scalatest.{Matchers, WordSpec}
//
//import sangria.ast.Document
//import sangria.macros._
//import sangria.execution.Executor
//import sangria.execution.deferred.DeferredResolver
//import sangria.marshalling.circe._
//
//import io.circe._
//import io.circe.parser._
//
//import scala.concurrent.Await
//import scala.concurrent.duration._
//import scala.concurrent.ExecutionContext.Implicits.global
//
//import .StarWarsSchema
//
//class SchemaSpec extends WordSpec with Matchers {
//  "StartWars Schema" should {
//    "correctly identify R2-D2 as the hero of the Star Wars Saga" in {
//      val query =
//        graphql"""
//         query HeroNameQuery {
//           hero {
//             name
//           }
//         }
//       """
//
//      executeQuery(query) should be (parse(
//        """
//         {
//           "data": {
//             "hero": {
//               "name": "R2-D2"
//             }
//           }
//         }
//       """).right.get)
//    }
//
//    "allow to fetch Han Solo using his ID provided through variables" in {
//      val query =
//        graphql"""
//         query FetchSomeIDQuery($$humanId: String!) {
//           human(id: $$humanId) {
//             name
//             friends {
//               id
//               name
//             }
//           }
//         }
//       """
//
//      executeQuery(query, vars = Json.obj("humanId" → Json.fromString("1002"))) should be (parse(
//        """
//         {
//           "data": {
//             "human": {
//               "name": "Han Solo",
//               "friends": [
//                 {
//                   "id": "1000",
//                   "name": "Luke Skywalker"
//                 },
//                 {
//                   "id": "1003",
//                   "name": "Leia Organa"
//                 },
//                 {
//                   "id": "2001",
//                   "name": "R2-D2"
//                 }
//               ]
//             }
//           }
//         }
//        """).right.get)
//    }
//  }
//
//  def executeQuery(query: Document, vars: Json = Json.obj()) = {
//    val futureResult = Executor.execute(StarWarsSchema, query,
//      variables = vars,
//      userContext = new CharacterRepo,
//      deferredResolver = DeferredResolver.fetchers(SchemaDef.characters))
//
//    Await.result(futureResult, 10.seconds)
//  }
//}
