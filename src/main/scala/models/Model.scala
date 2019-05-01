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

class Model(_id: String, _name: String) {
  val id = this._id
  val name = this._name
}

object Model {

  def apply(id: String, name: String): Model = new Model(id, name)

  implicit val unmarshaller
    : Unmarshaller[HttpEntity, Either[circe.Error, List[Model]]] = {

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
          case _ =>
            for {
              id <- c.get[String]("id")
              name <- c.get[String]("name")
              roles <- c.get[List[EntityRole]]("roles")
            } yield SimpleEntity(id, name, roles)
        }).getOrElse(Left(DecodingFailure("Not a valid model!", List())))
    }

    Unmarshaller.byteStringUnmarshaller.map {
      case ByteString.empty ⇒ throw Unmarshaller.NoContentException
      case data ⇒
        parser.decode[List[Model]](data.decodeString(Charset.forName("UTF-8")))
    }
  }
}

case class EntityRole(override val id: String, override val name: String)
    extends Model(id, name)

case class Sublist(id: Int, canonicalForm: String, list: List[String])

sealed class Entity(_id: String, _name: String, _roles: List[EntityRole])
    extends Model(_id, _name) {
  val roles: List[EntityRole] = this._roles
}

case class Intent(override val id: String,
                  override val name: String,
                  utterances: List[String] = List.empty)
    extends Model(id, name)

case class SimpleEntity(override val id: String,
                        override val name: String,
                        override val roles: List[EntityRole])
    extends Entity(id, name, roles)

case class ListEntity(override val id: String,
                      override val name: String,
                      override val roles: List[EntityRole],
                      sublists: List[Sublist])
    extends Entity(id, name, roles)

case class CompositeEntity(override val id: String,
                           override val name: String,
                           override val roles: List[EntityRole],
                           children: List[Entity])
    extends Entity(id, name, roles)

case class HierarchicalEntity(override val id: String,
                              override val name: String,
                              override val roles: List[EntityRole],
                              children: List[Entity])
    extends Entity(id, name, roles)
