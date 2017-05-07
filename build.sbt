organization := "com.github.wilaszekg"
name := "scala-dynamic-di"
version := "0.0.5-SNAPSHOT"

scalaVersion := "2.12.2"

description := "Dynamic dependency injection for Akka"
homepage := Some(url("https://github.com/wilaszekg/Scala-Dynamic-DI"))
licenses := Seq("Apache License, ASL Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

val AkkaVersion: String = "2.5.1"
val ShapelessVersion = "2.3.2"
val CatsVersion = "0.9.0"
val ScalTestVersion = "3.0.3"

libraryDependencies += "com.chuusai" %% "shapeless" % ShapelessVersion
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % AkkaVersion

libraryDependencies += "org.scalatest" %% "scalatest" % ScalTestVersion % "test"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % "test"
libraryDependencies += "org.typelevel" %% "cats" % CatsVersion

// Publishing

publishMavenStyle := true
publishArtifact in Test := false
pomIncludeRepository := (_ => false)
publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

startYear := Some(2015)

organizationHomepage := Some(url("https://github.com/wilaszekg"))
scmInfo := Some(ScmInfo(
  browseUrl = url("https://github.com/wilaszekg/Scala-Dynamic-DI"),
  connection = "scm:git:git@github.com:wilaszekg/Scala-Dynamic-DI.git"
))

pomExtra := <xml:group>
  <developers>
    <developer>
      <id>GrzegorzWilaszek</id>
      <name>Grzegorz Wilaszek</name>
    </developer>
  </developers>
</xml:group>