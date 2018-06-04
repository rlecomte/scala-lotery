package io.fplotery

import cats.effect.IO
import cats.effect.concurrent.Ref
import io.circe.syntax._
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class WinnerService(cache: Ref[IO, Vector[User]]) extends Http4sDsl[IO] {

  object NbWinner extends QueryParamDecoderMatcher[Int]("nb")

  val service: HttpService[IO] = {
    HttpService[IO] {
      case GET -> Root / "winners" :? NbWinner(nb) =>
        Ok {
          for {
            users <- cache.get
            winners <- IO(util.Random.shuffle(users).take(nb))
            json <- IO(winners.asJson)
          } yield json
        }
    }
  }
}
