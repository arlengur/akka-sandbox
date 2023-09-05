package ru.arlen.base

import akka.actor.typed._
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object OopAndFuncStyles {
  object func_style {
    object Echo {
      def apply(): Behavior[String] = Behaviors.setup { ctx =>
        Behaviors.receiveMessage {
          case msg =>
            ctx.log.info(s"func: $msg")
            Behaviors.same
        }
      }
    }
  }

  // ООП стиль акторов
  object oop_style {
    class Echo(ctx: ActorContext[String]) extends AbstractBehavior[String](ctx) {
      def onMessage(msg: String): Behavior[String] = {
        ctx.log.info(s"oop: $msg")
        this
      }
    }

    object Echo {
      def apply(): Behavior[String] = Behaviors.setup { ctx =>
        new Echo(ctx)
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem[String](func_style.Echo(), "func_actor")
    system ! "Hello"
    Thread.sleep(3000)
    system.terminate()
  }

}
