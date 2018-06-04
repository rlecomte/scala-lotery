package io.fplotery

import cats.effect.IO
import io.circe.{Decoder, Encoder, Json}
import org.http4s.Uri
import org.http4s.client.Client
import cats.implicits._
import io.circe.syntax._
import org.http4s.circe._

case class EventBritConf(orgaId: String, token: String)
case class User(firstname: String, lastname: String)

object User {

  implicit val circeUserDecoder: Decoder[User] = Decoder.instance { c =>
    (
      c.get[String]("first_name"),
      c.get[String]("last_name")
    ).mapN(User.apply)
  }.prepare(_.downField("profile"))

  implicit val circeUserEncoder: Encoder[User] = Encoder.instance { user =>
    Json.obj(
      "first_name" -> user.firstname.asJson,
      "last_name" -> user.lastname.asJson
    )
  }
}

class EventBriteApi(client: Client[IO], conf: EventBritConf) {

  val loadAttendees: IO[Vector[User]] = extractId.flatMap(getAttendees)

  private def extractId: IO[Int] = for {
    payload <- client.expect[Json](
      Uri.unsafeFromString(s"https://www.eventbriteapi.com/v3/events/search/?sort_by=date&organizer.id=${conf.orgaId}&token=${conf.token}")
    )
    extractId <- IO.fromEither(payload.hcursor.downField("events").downN(0).get[Int]("id"))
  } yield extractId

  private def getAttendees(id: Int): IO[Vector[User]] = {
    val urlAttendees = s"https://www.eventbriteapi.com/v3/events/$id/attendees?token=${conf.token}"

    val extractPageCount: IO[Int] = for {
      json <- client.expect[Json](urlAttendees)
      nbPage <- IO.fromEither(json.hcursor.downField("pagination").get[Int]("page_number"))
    } yield nbPage

    def extractUser(page: Int): IO[Vector[User]] = for {
      json <- client.expect[Json](urlAttendees ++ s"&page=$page")
      users <- IO.fromEither(json.hcursor.get[Vector[User]]("attendees"))
    } yield users

    import scala.concurrent.ExecutionContext.Implicits.global
    for {
      pageCount <- extractPageCount
      allUsers <- Vector.range(1, pageCount).parTraverse(extractUser)
    } yield allUsers.flatten
  }
}
