import Dependencies._

ThisBuild / scalaVersion     := "2.12.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "org.amitayh"
ThisBuild / organizationName := "amitayh"

lazy val root = (project in file("."))
  .settings(
    name := "catska",
    libraryDependencies ++= Seq(fs2, kafkaClients, scalaTest % Test)
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
