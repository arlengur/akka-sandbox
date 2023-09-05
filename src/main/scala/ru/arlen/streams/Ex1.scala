package ru.arlen.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object Ex1 {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("fusion")
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val source: Source[Int, NotUsed] = Source(1 to 5)
    val flow = Flow[Int].map(x => x + 1)
    val sink = Sink.foreach[Int](println)

    val runnable = source.via(flow).to(sink)

//    runnable.run()

    Source(1 to 3)
      .map(e => {println(s"Flow A: $e"); e}).async
      .map(e => {println(s"Flow B: $e"); e}).async
      .map(e => {println(s"Flow C: $e"); e}).async
      .runWith(Sink.ignore)
  }
}
