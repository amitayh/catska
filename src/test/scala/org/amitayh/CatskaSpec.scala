package org.amitayh

import java.util.{Properties, UUID}

import cats.effect.{ContextShift, IO}
import cats.implicits._
import org.amitayh.Catska.Topic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.Serdes.StringSerde
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class CatskaSpec(implicit ee: ExecutionEnv) extends Specification with KafkaDockerKit {

  implicit val contextShift: ContextShift[IO] =
    IO.contextShift(ee.executionContext)

  private val topic = Topic(
    name = "test-topic",
    keySerde = new StringSerde,
    valueSerde = new StringSerde)

  private val bootstrapServers = s"localhost:$KafkaAdvertisedPort"

  private val producerProps = {
    val props = new Properties
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    props
  }

  private val consumerProps = {
    val props = new Properties
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    props.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString)
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    props
  }

  override val topics: Set[Topic[_, _]] = Set(topic)

  "produce and consume messages from Kafka" in {
    val prog = waitUntilContainerIsReady *>
      Catska.producer[IO, String, String](topic, producerProps).use { producer =>
        producer.send("key1", "value1") *>
          producer.send("key1", "value2") *>
          Catska.subscribe[IO, String, String](topic, consumerProps).take(2).compile.toList
      }

    prog.unsafeRunSync() must equalTo("key1" -> "value1" :: "key1" -> "value2" :: Nil)
  }

}
