package org.amitayh

import cats.effect.IO
import cats.implicits._
import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.specs2.DockerTestKit
import com.whisk.docker.{DockerContainer, DockerFactory, DockerReadyChecker}
import org.amitayh.Catska.Topic

trait KafkaDockerKit extends DockerTestKit {

  def topics: Set[Topic]

  val KafkaAdvertisedPort = 9092
  val ZookeeperDefaultPort = 2181

  override implicit def dockerFactory: DockerFactory = {
    val client = DefaultDockerClient.fromEnv().build()
    new SpotifyDockerFactory(client)
  }

  lazy val kafkaContainer: DockerContainer = DockerContainer("spotify/kafka")
    .withPorts(KafkaAdvertisedPort -> Some(KafkaAdvertisedPort), ZookeeperDefaultPort -> None)
    .withEnv(
      s"ADVERTISED_PORT=$KafkaAdvertisedPort",
      s"ADVERTISED_HOST=${dockerExecutor.host}",
      s"TOPICS=${topics.map(_.name).mkString(",")}")
    .withReadyChecker(DockerReadyChecker.LogLineContains("kafka entered RUNNING state"))

  override def dockerContainers: List[DockerContainer] =
    kafkaContainer :: super.dockerContainers

  def waitUntilContainerIsReady: IO[Unit] =
    IO.fromFuture(IO(isContainerReady(kafkaContainer)))
      .ifM(IO.unit, IO.raiseError(UnableToStartKafka))

}

case object UnableToStartKafka
  extends RuntimeException("Unable to start Kafka container")
