package ru.arlen.persistent

import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior, Props}
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{EventEnvelope, PersistenceQuery}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.stream.scaladsl.Source
import ru.arlen.persistent.AkkaPersistenceExtended.CalculatorRepository.{getLatestsOffsetAndResult, initDatabase, updatedResultAndOffset}
import scalikejdbc.DB.using
import scalikejdbc.{ConnectionPool, ConnectionPoolSettings, DB}

object AkkaPersistenceExtended {
  sealed trait Command
  case class Add(amount: Int) extends Command
  case class Multiply(amount: Int) extends Command
  case class Divide(amount: Int) extends Command

  sealed trait Event
  case class Added(id: Int, amount: Int) extends Event
  case class Multiplied(id: Int, amount: Int) extends Event
  case class Divided(id: Int, amount: Int) extends Event

  trait CborSerialization

  object CalculatorWriteSide {
    final case class State(value: Int) extends CborSerialization {
      def add(amount: Int): State = copy(value = value + amount)

      def multiply(amount: Int): State = copy(value = value * amount)

      def divide(amount: Int): State = copy(value = value / amount)
    }

    def handleCommand(persistenceId: String,
                      state: State,
                      command: Command,
                      ctx: ActorContext[Command]): Effect[Event, State] =
      command match {
        case Add(amount) =>
          ctx.log.info(s"receive adding  for number: $amount and state is ${state.value}")
          val added = Added(persistenceId.toInt, amount)
          Effect
            .persist(added)
            .thenRun {
              x => ctx.log.info(s"The state result is ${x.value}")
            }
        case Multiply(amount) =>
          ctx.log.info(s"receive multiplying for number: $amount and state is ${state.value}")
          val multiplied = Multiplied(persistenceId.toInt, amount)
          Effect
            .persist(multiplied)
            .thenRun {
              x => ctx.log.info(s"The state result is ${x.value}")
            }
        case Divide(amount) =>
          ctx.log.info(s"receive dividing  for number: $amount and state is ${state.value}")
          val divided = Divided(persistenceId.toInt, amount)
          Effect
            .persist(divided)
            .thenRun {
              x => ctx.log.info(s"The state result is ${x.value}")
            }
      }

    def handleEvent(state: State, event: Event, ctx: ActorContext[Command]): State =
      event match {
        case Added(_, amount) =>
          ctx.log.info(s"Handling event Added is: $amount and state is ${state.value}")
          state.add(amount)
        case Multiplied(_, amount) =>
          ctx.log.info(s"Handling event Multiplied is: $amount and state is ${state.value}")
          state.multiply(amount)
        case Divided(_, amount) =>
          ctx.log.info(s"Handling event Divided is: $amount and state is ${state.value}")
          state.divide(amount)
      }

    def apply(): Behavior[Command] =
      Behaviors.setup { ctx =>
        val persId = "001"
        EventSourcedBehavior[Command, Event, State](
          PersistenceId.ofUniqueId(persId),
          State(0),
          (state, command) => handleCommand(persId, state, command, ctx),
          (state, event) => handleEvent(state, event, ctx)
        )
      }
  }

  case class CalculatorReadSide(system: ActorSystem[NotUsed]) {
    initDatabase

    implicit val materializer = system.classicSystem
    var (offset, latestCalculatedResult) = getLatestsOffsetAndResult
    val startOffset: Int = if (offset == 1) 1 else offset + 1

    val readJournal: CassandraReadJournal = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

    val source: Source[EventEnvelope, NotUsed] =
    readJournal.eventsByPersistenceId("001", startOffset, Long.MaxValue)

    source
      .map { x =>
        println(s"mylog: ${x.toString()}")
        x
      }
      .runForeach {
        event =>
          event.event match {
            case Added(_, amount) =>
              latestCalculatedResult += amount
              updatedResultAndOffset(latestCalculatedResult, event.sequenceNr)
              println(s"Log from Added: $latestCalculatedResult")
            case Multiplied(_, amount) =>
              latestCalculatedResult *= amount
              updatedResultAndOffset(latestCalculatedResult, event.sequenceNr)
              println(s"Log from Multiplied: $latestCalculatedResult")
            case Divided(_, amount) =>
              latestCalculatedResult /= amount
              updatedResultAndOffset(latestCalculatedResult, event.sequenceNr)
              println(s"Log from Divided: $latestCalculatedResult")
            case e => println(s"Log: $e")
          }
      }
  }

  object CalculatorRepository {
    def initDatabase: Unit = {
      Class.forName("org.postgresql.Driver")
      val poolSettings = ConnectionPoolSettings(initialSize = 10, maxSize = 100)
      ConnectionPool.singleton("jdbc:postgresql://localhost:5432/demo", "docker", "docker", poolSettings)
    }

    def getLatestsOffsetAndResult: (Int, Double) = {
      val entities =
        DB readOnly { session =>
          session.list("select * from public.result where id = 1;") {
            row =>
              (
                row.int("write_side_offset"),
                row.double("calculated_value"))
          }
        }
      entities.head
    }

    def updatedResultAndOffset(calculated: Double, offset: Long): Unit = {
      using(DB(ConnectionPool.borrow())) {
        db =>
          db.autoClose(true)
          db.localTx {
            _.update("update public.result set calculated_value = ?, write_side_offset = ? where id = 1", calculated, offset)
          }
      }
    }
  }

  def apply(): Behavior[NotUsed] =
    Behaviors.setup {
      ctx =>
        val writeAcorRef = ctx.spawn(CalculatorWriteSide(), "Calculator", Props.empty)
        writeAcorRef ! Add(10)
        writeAcorRef ! Multiply(2)
        writeAcorRef ! Divide(5)

        Behaviors.same
    }

  def main(args: Array[String]): Unit = {
    val system: ActorSystem[NotUsed] = ActorSystem(AkkaPersistenceExtended(), "AkkaPersistenceExtended")
    CalculatorReadSide(system)
  }
}
