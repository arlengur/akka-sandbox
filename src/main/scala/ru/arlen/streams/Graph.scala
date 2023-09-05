package ru.arlen.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, Zip}

object AkkaStreamsGraph {
  implicit val system: ActorSystem = ActorSystem("Graph")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val graph =
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val input = builder.add(Source(1 to 5))

      val broadcast = builder.add(Broadcast[Int](2))

      val increment = builder.add(Flow[Int].map(x => x + 1))
      val multiplier = builder.add(Flow[Int].map(x => x * 10))

      val zip = builder.add(Zip[Int, Int])

      val output = builder.add(Sink.foreach[(Int, Int)](println))

      input ~> broadcast

      broadcast.out(0) ~> increment ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1

      zip.out ~> output

      ClosedShape
    }

  def main(args: Array[String]): Unit = {
    RunnableGraph.fromGraph(graph).run()
  }
}
