package ru.arlen.base

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, DeathPactException, SupervisorStrategy}

sealed trait Msg
case class Fail(text: String) extends Msg
case class Hello(text: String) extends Msg

object Worker {
  def apply(): Behavior[Msg] =
    Behaviors.receiveMessage {
      case Fail(text) =>
        throw new RuntimeException(text)
      case Hello(text) =>
        println(text)
        Behaviors.same
    }
}

object MiddleManagement {
  def apply(): Behavior[Msg] =
    Behaviors.setup[Msg] { context =>
      context.log.info("Middle Management starting up")
      val child = context.spawn(Worker(), "child")
      context.watch(child)

      Behaviors.receiveMessage[Msg] { message =>
        child ! message
        Behaviors.same
      }
    }
}

object Boss {
  def apply(): Behavior[Msg] =
    Behaviors.supervise(
      Behaviors.setup[Msg] { ctx =>
        ctx.log.info("Boss starting up")
        val middleManagement = ctx.spawn(MiddleManagement(), "middle-management")
        ctx.watch(middleManagement)

        Behaviors.receiveMessage[Msg] { message =>
          middleManagement ! message
          Behaviors.same
        }
      }
    ).onFailure[DeathPactException](SupervisorStrategy.restart)
}

object StartStopProcessing extends App {
  def apply(): Behavior[NotUsed] =
    Behaviors.setup { ctx =>
      val boss = ctx.spawn(Boss(), "boss-management")
      boss ! Hello("hi 1")
      boss ! Fail("ping")
      Thread.sleep(1000)

      boss ! Hello("hi 2")

      Behaviors.same
    }

  implicit val system: ActorSystem[NotUsed] = ActorSystem(StartStopProcessing(), "akka_typed")
  Thread.sleep(5000)
  system.terminate()
}
