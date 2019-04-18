package org.amitayh

import java.time.Duration
import java.util.Collections.singletonList
import java.util.Properties

import cats.effect._
import cats.effect.concurrent.Semaphore
import cats.syntax.apply._
import cats.syntax.functor._
import fs2._
import org.apache.kafka.clients.consumer._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.{Deserializer, Serializer}

import scala.collection.JavaConverters._
import scala.language.higherKinds

object Catska {

  case class Topic(name: String) extends AnyVal

  trait Producer[F[_], K, V] {
    def send(key: K, value: V): F[RecordMetadata]
  }

  object Producer {
    def apply[F[_]: Async, K, V](producer: KafkaProducer[K, V], topic: Topic): Producer[F, K, V] =
      (key: K, value: V) => Async[F].async { cb =>
        val record = new ProducerRecord(topic.name, key, value)
        producer.send(record, (metadata: RecordMetadata, exception: Exception) => {
          if (exception != null) cb(Left(exception))
          else cb(Right(metadata))
        })
      }
  }

  trait Consumer[F[_], K, V] {
    def subscribe(topic: Topic): Stream[F, (K, V)]
  }

  object Consumer {
    def apply[F[_]: Sync, K, V](consumer: KafkaConsumer[K, V], semaphore: Semaphore[F]): Consumer[F, K, V] =
      (topic: Topic) => for {
        _ <- Stream.eval(semaphore.withPermitDelay(consumer.subscribe(singletonList(topic.name))))
        records <- Stream.repeatEval(semaphore.withPermitDelay(consumer.poll(Duration.ofSeconds(1))))
        record <- Stream.emits(records.iterator.asScala.toSeq).filter(_.topic == topic.name)
      } yield record.key -> record.value
  }

  def producer[F[_]: Async, K: Serializer, V: Serializer]
    (topic: Topic, props: Properties): Resource[F, Producer[F, K, V]] = Resource {
    val create = Sync[F].delay(new KafkaProducer[K, V](props, Serializer[K], Serializer[V]))
    create.map { producer =>
      val close = Sync[F].delay(producer.close())
      (Producer(producer, topic), close)
    }
  }

  def consumer[F[_]: Concurrent, K: Deserializer, V: Deserializer]
    (topic: Topic, props: Properties): Resource[F, Consumer[F, K, V]] = Resource {
    val create = Sync[F].delay(new KafkaConsumer(props, Deserializer[K], Deserializer[V]))
    (create, Semaphore[F](1)).mapN {
      case (consumer, semaphore) =>
        val close = semaphore.withPermitDelay(consumer.close())
        (Consumer(consumer, semaphore), close)
    }
  }

  def subscribe[F[_]: Concurrent, K: Deserializer, V: Deserializer]
    (topic: Topic, props: Properties): Stream[F, (K, V)] =
    Stream.resource(consumer[F, K, V](topic, props)).flatMap(_.subscribe(topic))

  object Serializer {
    def apply[A](implicit serializer: Serializer[A]): Serializer[A] = serializer
  }

  object Deserializer {
    def apply[A](implicit deserializer: Deserializer[A]): Deserializer[A] = deserializer
  }

  implicit class SemaphoreOps[F[_]](val semaphore: Semaphore[F]) extends AnyVal {
    def withPermitDelay[A](a: => A)(implicit F: Sync[F]): F[A] =
      semaphore.withPermit(F.delay(a))
  }

}
