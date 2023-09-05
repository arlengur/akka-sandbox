package ru.arlen.base

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

sealed trait Command
case class StartChild(name: String) extends Command
case class SendMessageToChild(name: String, msg: String, num: Int) extends Command
case class StopChild(name: String) extends Command
case object Stop extends Command

object Parent {
  def apply(): Behavior[Command] = withChildren(Map())

  def withChildren(childs: Map[String, ActorRef[Command]]): Behavior[Command] =
    Behaviors.setup { ctx =>
      Behaviors.receiveMessage[Command] {
        case StartChild(name) =>
          ctx.log.info(s"Start child $name")
          val newChild = ctx.spawn(Child(), name)
          withChildren(childs + (name -> newChild))
        case msg@SendMessageToChild(name, _, i) =>
          ctx.log.info(s"Send message to child $name num=$i")
          val childOption = childs.get(name)
          childOption.foreach(childRef => childRef ! msg)
          Behaviors.same
        case StopChild(name) =>
          ctx.log.info(s"Stopping child with name $name")
          val childOption = childs.get(name)
          childOption.foreach(childRef => ctx.stop(childRef))
          Behaviors.same
        case Stop =>
          ctx.log.info("Stopped parent")
          Behaviors.stopped
      }
    }
}

object Child {
  def apply(): Behavior[Command] = Behaviors.setup { ctx =>
    Behaviors.receiveMessage { msg =>
      ctx.log.info(s"Child got message $msg")
      Behaviors.same
    }
  }
}

object StartStop extends App {
  def apply(): Behavior[NotUsed] =
    Behaviors.setup { ctx =>
      val parent = ctx.spawn(Parent(), "parent")
      parent ! StartChild("child1")
      parent ! SendMessageToChild("child1", "message to child1", 0)
      parent ! StopChild("child1")
      parent ! SendMessageToChild("child1", "message to child1", 1)
      Behaviors.same
    }

  implicit val system: ActorSystem[NotUsed] = ActorSystem(StartStop(), "start_stop")
  Thread.sleep(5000)
  system.terminate()
}
