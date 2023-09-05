package ru.arlen.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object Ex2 {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("fusion")
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val source: Source[Int, NotUsed] = Source(1 to 5)
    val flow1 = Flow[Int].map { x =>
      Thread.sleep(1000)
      x + 1
    }
    val flow2 = Flow[Int].map { x =>
      Thread.sleep(1000)
      x * 10
    }
    val sink = Sink.foreach[Int](println)

    source.async
      .via(flow1).async
      .via(flow2).async
      .to(sink)
      .run()
  }
}