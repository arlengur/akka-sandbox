package ru.arlen.base

import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import OopAndFuncStyles.func_style

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object Supervisor {
  def apply(): Behavior[SpawnProtocol.Command] = Behaviors.setup { ctx =>
    ctx.log.info(ctx.self.toString)
    SpawnProtocol()
  }


  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem[SpawnProtocol.Command](Supervisor(), "Echo")
    implicit val timeout: Timeout = Timeout(3 seconds)

    val echo: Future[ActorRef[String]] = system.ask(
      SpawnProtocol.Spawn(func_style.Echo(), "Echo", Props.empty, _))

    implicit val ec = system.executionContext
    for (ref <- echo)
      ref ! "Hello from ask"
  }

}
