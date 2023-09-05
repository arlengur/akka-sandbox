package ru.arlen.base

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object Counter {
  sealed trait CounterProtocol

  object CounterProtocol {
    final case object Inc extends CounterProtocol
    final case class GetCounter(replyto: ActorRef[Int]) extends CounterProtocol
  }

  import CounterProtocol._

  def apply(init: Int): Behavior[CounterProtocol] = inc(init)

  def inc(counter: Int): Behavior[CounterProtocol] = Behaviors.setup { _ =>
    Behaviors.receiveMessage {
      case Inc =>
        inc(counter + 1)
      case GetCounter(replyTo) =>
        replyTo ! counter
        Behaviors.same
    }
  }

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[CounterProtocol] = ActorSystem[CounterProtocol](Counter(0), "Counter")
    implicit val timeout: Timeout = Timeout(3 seconds)
    implicit val sc: Scheduler = system.scheduler

    system ! Inc
    system ! Inc
    val res = system ? GetCounter
    println(Await.result(res, 1 seconds))
    system.terminate()
  }
}
