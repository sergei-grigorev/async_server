name := "async_server"

version := "0.1"

scalaVersion := "2.12.8"

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.1")

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-zio" % "1.0-RC5",
  "org.scalaz" %% "scalaz-zio-interop-cats" % "1.0-RC5",
  "org.typelevel" %% "cats-core" % "1.6.0",
  "org.typelevel" %% "cats-effect" % "1.3.1",
  "eu.timepit" %% "refined" % "0.9.5",
  "org.scalatest" %% "scalatest" % "3.0.7" % Test,
  "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
  "org.typelevel" %% "cats-kernel-laws" % "1.6.0" % Test
)