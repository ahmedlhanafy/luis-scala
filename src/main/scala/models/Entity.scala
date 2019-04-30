package models

import java.nio.charset.Charset

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.util.ByteString
import io.circe
import io.circe.{Decoder, DecodingFailure, HCursor, parser}
import cats.syntax.functor._
import io.circe.generic.semiauto
import io.circe.generic.auto._

class Model(id: String, name: String)

case class EntityRole(id: String, name: String)

case class Sublist(id: String, canonicalForm: String, list: List[String])

sealed class Entity(id: String, name: String, roles: List[EntityRole])
    extends Model(id, name)

case class SimpleEntity(id: String, name: String, roles: List[EntityRole])
    extends Entity(id, name, roles)

case class ListEntity(id: String,
                      name: String,
                      roles: List[EntityRole],
                      sublists: List[Sublist])
    extends Entity(id, name, roles)
//
//object ParentEntityType extends Enumeration {
//  val COMPOSITE, HIERARCHICAL = Value
//}
//
//case class ParentEntity(id: String,
//                        name: String,
//                        roles: List[EntityRole],
//                        entityType: ParentEntityType.Value,
//                        children: List[Entity])
//    extends Entity

object Model {

  implicit val unmarshaller
    : Unmarshaller[HttpEntity, Either[circe.Error, List[Model]]] =
    Unmarshaller.byteStringUnmarshaller.map {
      case ByteString.empty ⇒ throw Unmarshaller.NoContentException
      case data ⇒
        parser.decode[List[Model]](data.decodeString(Charset.forName("UTF-8")))
    }

  implicit def decoder[T <: Model]: Decoder[T] =
    Decoder.instance(
      c =>
        c.downField("typeId")
          .as[Int]
          .flatMap {
            case 1 => c.as[SimpleEntity]
            case 5 => c.as[ListEntity]
            case 0 => c.as[Model]
          }
          .right
          .map(_.asInstanceOf[T])
    )
}
