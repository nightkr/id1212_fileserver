scalaVersion in ThisBuild := "2.12.4"

lazy val sharedSettings = Seq(
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-api" % "1.7.5",
    "com.google.inject" % "guice" % "4.1.0",
    "com.jsuereth" %% "scala-arm" % "2.0",
    "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
    "org.scalatest" %% "scalatest" % "3.0.4" % "test",
    "org.scalacheck" %% "scalacheck" % "1.13.4" % "test"
  ),
  scalacOptions ++= Seq("-deprecation", "-feature"),
  fork in run := true,
  // connectInput in run := true,
  cancelable in Global := true
)

lazy val common = project.settings(sharedSettings: _*)

lazy val server = project
  .settings(sharedSettings: _*)
  .dependsOn(common)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick" % "3.2.1",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.2.1",
      "org.postgresql" % "postgresql" % "42.1.4",
      "com.h2database" % "h2" % "1.4.196",
      "commons-codec" % "commons-codec" % "1.11"
    )
  )

lazy val client = project
  .settings(sharedSettings: _*)
  .dependsOn(common)
  .settings(
    connectInput in run := true
  )

lazy val root = project.in(file(".")).aggregate(common, server, client)
