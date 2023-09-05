package ru.arlen.base

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

object MutableState {
  sealed trait Command

  case class Deposite(v: Integer) extends Command

  case class Withdraw(v: Int) extends Command

  case class Get() extends Command

  object Account {
    def apply(m: Int): Behavior[Command] = Behaviors.setup { ctx =>
      var amount: Int = m

      Behaviors.receiveMessage {
        case Deposite(v) =>
          amount = amount + v
          ctx.log.info(s"Deposite $v to amount $amount. Total state is $amount")
          Behaviors.same
        case Withdraw(v) =>
          amount = amount - v
          ctx.log.info(s"Withdraw $v from amount $amount. Total state is $amount")
          Behaviors.same
        case Get() =>
          ctx.log.info(s"Total state is $amount")
          Behaviors.same
      }
    }
  }

  def apply(): Behavior[NotUsed] =
    Behaviors.setup { ctx =>
      val account1 = ctx.spawn(Account(2000), "account_state_actor1")
      val account2 = ctx.spawn(Account(42), "account_state_actor2")

      account1 ! Get()
      account1 ! Deposite(1)
      account2 ! Get()
      account1 ! Withdraw(1)

      Behaviors.same
    }

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[NotUsed] = ActorSystem(MutableState(), "two_accounts")
    Thread.sleep(5000)
    system.terminate()
  }
}
