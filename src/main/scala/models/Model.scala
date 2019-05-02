package models

import java.nio.charset.Charset

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.util.ByteString
import io.circe
import io.circe.{Decoder, DecodingFailure, HCursor, parser}
import io.circe.generic.auto._

class Model(val id: String, val name: String) {}

object Model {

  def apply(id: String, name: String): Model = new Model(id, name)

  implicit val decoder: Decoder[Model] = (c: HCursor) => {
    (for {
      typeId <- c.get[Int]("typeId")
    } yield
      typeId match {
        case 0 =>
          for {
            id <- c.get[String]("id")
            name <- c.get[String]("name")
          } yield Intent(id, name)
        case 5 =>
          for {
            id <- c.get[String]("id")
            name <- c.get[String]("name")
            roles <- c.get[List[EntityRole]]("roles")
            subLists <- c.get[List[Sublist]]("subLists")
          } yield ListEntity(id, name, roles, subLists)
        case 3 =>
          for {
            id <- c.get[String]("id")
            name <- c.get[String]("name")
            roles <- c.get[List[EntityRole]]("roles")
            children <- c.get[List[ChildEntity]]("children")
          } yield HierarchicalEntity(id, name, roles, children)
        case 4 =>
          for {
            id <- c.get[String]("id")
            name <- c.get[String]("name")
            roles <- c.get[List[EntityRole]]("roles")
            children <- c.get[List[ChildEntity]]("children")
          } yield CompositeEntity(id, name, roles, children)
        case _ =>
          for {
            id <- c.get[String]("id")
            name <- c.get[String]("name")
            roles <- c.get[List[EntityRole]]("roles")
          } yield SimpleEntity(id, name, roles)
      }).getOrElse(Left(DecodingFailure("Not a valid model!", List())))
  }

  implicit val unmarshaller
    : Unmarshaller[HttpEntity, Either[circe.Error, Model]] =
    Unmarshaller.byteStringUnmarshaller.map {
      case ByteString.empty ⇒ throw Unmarshaller.NoContentException
      case data ⇒
        parser.decode[Model](data.decodeString(Charset.forName("UTF-8")))
    }

  implicit val listUnmarshaller
    : Unmarshaller[HttpEntity, Either[circe.Error, List[Model]]] =
    Unmarshaller.byteStringUnmarshaller.map {
      case ByteString.empty ⇒ throw Unmarshaller.NoContentException
      case data ⇒
        parser.decode[List[Model]](data.decodeString(Charset.forName("UTF-8")))
    }

}

case class EntityRole(override val id: String, override val name: String)
    extends Model(id, name)

case class Sublist(id: Int, canonicalForm: String, list: List[String])

sealed class Entity(override val id: String,
                    override val name: String,
                    val roles: List[EntityRole])
    extends Model(id, name) {}

case class Intent(override val id: String,
                  override val name: String,
                  utterances: List[Utterance] = List.empty)
    extends Model(id, name)

case class SimpleEntity(override val id: String,
                        override val name: String,
                        override val roles: List[EntityRole])
    extends Entity(id, name, roles)

case class ListEntity(override val id: String,
                      override val name: String,
                      override val roles: List[EntityRole],
                      subLists: List[Sublist])
    extends Entity(id, name, roles)

case class ChildEntity(override val id: String, override val name: String)
    extends Model(id, name)

case class CompositeEntity(override val id: String,
                           override val name: String,
                           override val roles: List[EntityRole],
                           children: List[ChildEntity])
    extends Entity(id, name, roles)

case class HierarchicalEntity(override val id: String,
                              override val name: String,
                              override val roles: List[EntityRole],
                              children: List[ChildEntity])
    extends Entity(id, name, roles)
