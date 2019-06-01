name := "async_server"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-zio" % "1.0-RC4",
  "org.typelevel" %% "cats-core" % "1.6.0",
  "eu.timepit" %% "refined" % "0.9.5",
  "org.scalatest" %% "scalatest" % "3.0.7" % Test,
  "org.scalacheck" %% "scalacheck" % "1.14.0" % Test
)