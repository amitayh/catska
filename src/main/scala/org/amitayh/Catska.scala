package org.amitayh

import java.time.Duration
import java.util.Collections.singletonList
import java.util.Properties

import cats.effect._
import cats.syntax.functor._
import fs2._
import org.apache.kafka.clients.consumer._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}

import scala.collection.JavaConverters._
import scala.language.higherKinds

object Catska {

  case class Topic[K, V](name: String,
                         keySerde: Serde[K],
                         valueSerde: Serde[V]) {
    val keySerializer: Serializer[K] = keySerde.serializer
    val keyDeserializer: Deserializer[K] = keySerde.deserializer
    val valueSerializer: Serializer[V] = valueSerde.serializer
    val valueDeserializer: Deserializer[V] = valueSerde.deserializer
  }

  trait Producer[F[_], K, V] {
    def send(key: K, value: V): F[RecordMetadata]
  }

  object Producer {
    def apply[F[_]: Async, K, V](producer: KafkaProducer[K, V], topic: Topic[K, V]): Producer[F, K, V] =
      (key: K, value: V) => Async[F].async { cb =>
        val record = new ProducerRecord(topic.name, key, value)
        producer.send(record, (metadata: RecordMetadata, exception: Exception) => {
          if (exception != null) cb(Left(exception))
          else cb(Right(metadata))
        })
      }
  }

  trait Consumer[F[_], K, V] {
    def subscribe(topic: Topic[K, V]): Stream[F, (K, V)]
  }

  object Consumer {
    def apply[F[_]: Sync, K, V](consumer: KafkaConsumer[K, V]): Consumer[F, K, V] =
      (topic: Topic[K, V]) =>  for {
        _ <- Stream.eval(Sync[F].delay(consumer.subscribe(singletonList(topic.name))))
        records <- Stream.repeatEval(Sync[F].delay(consumer.poll(Duration.ofSeconds(1))))
        record <- Stream.emits(records.iterator.asScala.toSeq).filter(_.topic == topic.name)
      } yield record.key -> record.value
  }

  def producer[F[_]: Async, K, V](topic: Topic[K, V], props: Properties): Resource[F, Producer[F, K, V]] = Resource {
    val create = Sync[F].delay(new KafkaProducer[K, V](props, topic.keySerializer, topic.valueSerializer))
    create.map { producer =>
      val close = Sync[F].delay(producer.close())
      (Producer(producer, topic), close)
    }
  }

  def consumer[F[_]: Async, K, V](topic: Topic[K, V], props: Properties): Resource[F, Consumer[F, K, V]] = Resource {
    val create = Sync[F].delay(new KafkaConsumer(props, topic.keyDeserializer, topic.valueDeserializer))
    create.map { consumer =>
      val close = Sync[F].delay(consumer.close())
      (Consumer(consumer), close)
    }
  }

  def subscribe[F[_]: Async, K, V](topic: Topic[K, V], props: Properties): Stream[F, (K, V)] =
    Stream.resource(consumer[F, K, V](topic, props)).flatMap(_.subscribe(topic))

}
