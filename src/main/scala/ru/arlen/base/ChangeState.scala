package ru.arlen.base

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

import scala.language.postfixOps

object ChangeState {
  sealed trait WorkerProtocol

  object WorkerProtocol {
    case object Start extends WorkerProtocol
    case object StandBy extends WorkerProtocol
    case object Stop extends WorkerProtocol
  }

  import WorkerProtocol._
  def apply(): Behavior[WorkerProtocol] = idle()

  def idle(): Behavior[WorkerProtocol] = Behaviors.setup { ctx =>
    Behaviors.receiveMessage {
      case msg@Start =>
        ctx.log.info(msg.toString)
        workInProgress()
      case msg@StandBy =>
        ctx.log.info(msg.toString)
        idle()
      case msg@Stop =>
        ctx.log.info(msg.toString)
        Behaviors.stopped
    }
  }

  def workInProgress(): Behavior[WorkerProtocol] = Behaviors.setup { ctx =>
    Behaviors.receiveMessage {
      case Start =>
        ctx.log.info("workInProgress started")
        Behaviors.unhandled
      case StandBy =>
        ctx.log.info("go to standBy")
        idle()
      case Stop =>
        ctx.log.info("stopped")
        Behaviors.stopped
    }
  }

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[WorkerProtocol] = ActorSystem[ChangeState.WorkerProtocol](ChangeState(), "ChangeState")

      system ! Start
      system ! StandBy
      system ! Stop
  }
}
