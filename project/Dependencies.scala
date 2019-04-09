import sbt._

object Dependencies {
  lazy val fs2          = "co.fs2"            %%  "fs2-core"      % "1.0.1"
  lazy val kafkaClients = "org.apache.kafka"  %   "kafka-clients" % "2.2.0"

  lazy val scalaTest    = "org.scalatest"     %% "scalatest"      % "3.0.5"
}
