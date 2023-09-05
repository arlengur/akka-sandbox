package ru.arlen.base

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import Dispatcher.DispatcherCommand.{LogWork, ParseUrl, TaskDispatcher}
import Dispatcher.LogWorker.{Log, LogResponse}
import Dispatcher.ParseUrlWorker.{Parse, ParseResponse}

object Dispatcher {
  object DispatcherCommand {
    sealed trait TaskDispatcher

    case class ParseUrl(url: String) extends TaskDispatcher

    case class LogWork(str: String) extends TaskDispatcher

    case class LogResponseWrapper(msg: LogResponse) extends TaskDispatcher

    case class ParseResponseWrapper(msg: ParseResponse) extends TaskDispatcher

    def apply(): Behavior[TaskDispatcher] = Behaviors.setup { ctx =>
      val logAdapter = ctx.messageAdapter[LogResponse](rs => LogResponseWrapper(rs))
      val parseAdapter = ctx.messageAdapter[ParseResponse](rs => ParseResponseWrapper(rs))

      Behaviors.receiveMessage {
        case ParseUrl(url) =>
          val ref = ctx.spawn(ParseUrlWorker(), s"ParseWorker-${java.util.UUID.randomUUID().toString}")
          ctx.log.info(s"Dispatcher received url $url")
          ref ! Parse(url, parseAdapter)
          Behaviors.same
        case LogWork(str) =>
          val ref = ctx.spawn(LogWorker(), s"LogWorker-${java.util.UUID.randomUUID().toString}")
          ctx.log.info(s"Dispatcher received log $str")
          ref ! Log(str, logAdapter)
          Behaviors.same
        case LogResponseWrapper(_) =>
          ctx.log.info("Log done")
          Behaviors.same
        case ParseResponseWrapper(_) =>
          ctx.log.info("Parse done")
          Behaviors.same
      }
    }
  }

  object LogWorker {
    sealed trait LogRequest

    case class Log(str: String, replyTo: ActorRef[LogResponse]) extends LogRequest

    sealed trait LogResponse

    case object LogDone extends LogResponse

    def apply(): Behavior[LogRequest] = Behaviors.setup { ctx =>
      Behaviors.receiveMessage {
        case Log(_, replyTo) =>
          ctx.log.info("log work in progress")
          replyTo ! LogDone
          Behaviors.stopped
      }
    }
  }

  object ParseUrlWorker {
    sealed trait ParseRequest

    case class Parse(url: String, replyTo: ActorRef[ParseResponse]) extends ParseRequest

    sealed trait ParseResponse

    case object ParseDone extends ParseResponse

    def apply(): Behavior[ParseRequest] = Behaviors.setup { ctx =>
      Behaviors.receiveMessage {
        case Parse(_, replyTo) =>
          ctx.log.info("parse work in progress")
          replyTo ! ParseDone
          Behaviors.stopped
      }
    }
  }

  def apply(): Behavior[NotUsed] =
    Behaviors.setup { ctx =>
      val dispatcher = ctx.spawn(DispatcherCommand(), "dispatcher")

      dispatcher ! LogWork("log line")
      dispatcher ! ParseUrl("parse url")

      Behaviors.same
    }

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[NotUsed] = ActorSystem(Dispatcher(), "dispatcher_actor_system")

    //    implicit val system: ActorSystem[TaskDispatcher] = ActorSystem[TaskDispatcher](DispatcherCommand(), "dispatcher_actor_system")
    //
    //    system ! LogWork("log line")
    //    system ! ParseUrl("parse url")

    Thread.sleep(3000)
    system.terminate()
  }
}
