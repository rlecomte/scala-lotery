val Http4sVersion = "0.18.12"
val Specs2Version = "4.2.0"
val LogbackVersion = "1.2.3"

lazy val root = (project in file("."))
  .settings(
    organization := "io",
    name := "fp-lotery",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.6",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server" % Http4sVersion exclude("org.typelevel", "cats-effect"),
      "org.http4s"      %% "http4s-circe"        % Http4sVersion exclude("org.typelevel", "cats-effect"),
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion exclude("org.typelevel", "cats-effect"),
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion,
      "org.typelevel" %% "cats-effect" % "1.0.0-RC2",
      "org.http4s" %% "http4s-blaze-client" % Http4sVersion exclude("org.typelevel", "cats-effect")
    )
  )

