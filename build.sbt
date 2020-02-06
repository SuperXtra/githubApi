name := """github-service"""
organization := "scalac"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "2.13.0"

PlayKeys.devSettings := Seq("play.server.http.port" -> "8080")


libraryDependencies ++= Seq(
  guice,
  ws,
  ehcache,
  "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test,
  "com.github.pureconfig" %% "pureconfig" % "0.12.2",
  specs2 % Test,
  "org.scalactic" %% "scalactic" % "3.1.0",
  "org.scalatest" %% "scalatest" % "3.1.0" % Test,
  "org.typelevel" %% "cats-core" % "2.0.0",
  "de.leanovate.play-mockws" %% "play-mockws" % "2.7.1" % Test,
  "com.iterable" %% "swagger-play" % "2.0.1"
)
scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-Xfatal-warnings"
)
