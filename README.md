# Catska [![Build Status](https://travis-ci.org/amitayh/catska.svg?branch=master)](https://travis-ci.org/amitayh/catska) [![codecov](https://codecov.io/gh/amitayh/catska/branch/master/graph/badge.svg)](https://codecov.io/gh/amitayh/catska)

A thin functional wrapper for Apache Kafka with [Cats](https://typelevel.org/cats/) and [FS2](https://fs2.io/).

All operations are pure and resource safe. The API surface is very minimal,
if you need more control you should probably use a different library.

## Consuming messages

Catska allows you to easily subscribe to a Kafka topic and get the stream of records:

```scala
import java.util.Properties
import java.util.UUID
import org.apache.kafka.common.serialization.Serdes._
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.amitayh.Catska
import cats.effect.IO

val topic = Catska.Topic(
  name = "some-topic",
  keySerde = new StringSerde,
  valueSerde = new StringSerde)

val props = new Properties
props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
props.put(ConsumerConfig.GROUP_ID_CONFIG, "some-consumer-group")

// Get records as a stream of key-value tuples - `Stream[IO, (String, String)]`
val records = Catska.subscribe[IO, String, String](topic, props)
```

## Producing messages

Creating a producer is also very simple

```scala
import ...

val topic = ...
val props = ...

// `Catska.producer` returns a producer as a `Resource[F, Producer[F, K, V]]`
Catska.producer[IO, String, String](topic, props).use { producer =>
  producer.send("key1", "value1") *>
    producer.send("key2", "value2")
}
```

### License

Copyright © 2019 Amitay Horwitz

Distributed under the MIT License
