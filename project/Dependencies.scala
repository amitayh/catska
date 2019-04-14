import sbt._

object Dependencies {
  private val versions = new {
    val fs2           = "1.0.1"
    val kafkaClients  = "2.2.0"
    val specs2        = "4.3.4"
    val dockerTestKit = "0.9.8"
  }

  lazy val fs2                = "co.fs2"            %%  "fs2-core"                    % versions.fs2
  lazy val kafkaClients       = "org.apache.kafka"  %   "kafka-clients"               % versions.kafkaClients

  lazy val specs2             = "org.specs2"        %% "specs2-core"                  % versions.specs2
  lazy val dockerTestKit      = "com.whisk"         %% "docker-testkit-specs2"        % versions.dockerTestKit
  lazy val dockerTestKitImpl  = "com.whisk"         %% "docker-testkit-impl-spotify"  % versions.dockerTestKit
}
