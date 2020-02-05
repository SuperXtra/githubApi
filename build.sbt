lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    name := """play-scala-starter-example""",
    version := "1.0-SNAPSHOT",
    scalaVersion := "2.13.1",
    libraryDependencies ++= Seq(
      guice,
      "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test,
      ws,
      ehcache,
      "com.github.pureconfig" %% "pureconfig" % "0.12.2",
      "org.scalactic" %% "scalactic" % "3.1.0",
      "org.scalatest" %% "scalatest" % "3.1.0" % Test,
      "org.typelevel" %% "cats-core" % "2.0.0"
    ),
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-Xfatal-warnings"
    )
  )
