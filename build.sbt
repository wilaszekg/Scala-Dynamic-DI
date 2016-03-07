name := "Scala-Dynamic-DI"

version := "1.0"

scalaVersion := "2.11.7"

val AkkaVersion: String = "2.3.11"

libraryDependencies += "com.chuusai" %% "shapeless" % "2.2.5"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % AkkaVersion

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.5" % "test"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % "test"

