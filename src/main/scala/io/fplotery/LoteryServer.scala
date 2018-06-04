package io.fplotery

import cats.effect.IO
import cats.effect.concurrent.Ref
import fs2.StreamApp
import org.http4s.client.blaze.Http1Client
import org.http4s.server.blaze.BlazeBuilder

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


object HelloWorldServer extends StreamApp[IO] {
  import scala.concurrent.ExecutionContext.Implicits.global
  def stream(args: List[String], requestShutdown: IO[Unit]) = ServerStream.stream(EventBritConf("foo", "bar"))
}

object ServerStream {

  def stream(conf: EventBritConf)(implicit ec: ExecutionContext): fs2.Stream[IO, StreamApp.ExitCode] = for {
    winnerService <- fs2.Stream.eval[IO, WinnerService](loadResources(conf))

    code <- BlazeBuilder[IO]
      .bindHttp(8080, "0.0.0.0")
      .mountService(winnerService.service, "/")
      .serve
  } yield code

  import scala.concurrent.ExecutionContext.Implicits.global
  private def loadResources(conf: EventBritConf): IO[WinnerService] = for {
    cache <- Ref.of[IO, Vector[User]](Vector())
    httpClient <- Http1Client[IO]()
    _ <- schedule(cache, new EventBriteApi(httpClient, conf)).start //start asynchronous task to fill users cache
  } yield new WinnerService(cache)

  private def schedule(cache: Ref[IO, Vector[User]], eventBriteApi: EventBriteApi): IO[Unit] = IO.suspend {
    for {
      _ <- IO(println("reload users cache..."))
      users <- eventBriteApi.loadAttendees
      _ <- cache.set(users)
      _ <- IO(println("cache sucessfully reloaded. Now sleep 1 hour."))
      _ <- IO.sleep(1.hour)
      _ <- schedule(cache, eventBriteApi)
    } yield ()
  }
}
