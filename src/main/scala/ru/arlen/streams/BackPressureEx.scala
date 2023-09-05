package ru.arlen.streams

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}

object BackPressureEx extends App {
  val source = Source(1 to 10)
  val flow = Flow[Int].map { el =>
    println(s"Flow inside: $el")
    el + 10
  }

  val flowWithBuffer = flow.buffer(3, overflowStrategy = OverflowStrategy.dropHead)
  val sink = Sink.foreach[Int] { el =>
    Thread.sleep(1000)
    println(s"Sink inside : $el")
  }

  implicit val system: ActorSystem = ActorSystem("fusion")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  source
    .via(flow).async
    .to(sink)
//    .run()

  source
    .via(flowWithBuffer).async
    .to(sink)
//    .run()
}
