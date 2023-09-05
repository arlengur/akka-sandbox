package ru.arlen.persistent

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior, Props}
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{EventEnvelope, PersistenceQuery}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ClosedShape, Graph}
import akka.{NotUsed, actor}
import slick.basic.DatabaseConfig
import slick.jdbc.{GetResult, JdbcProfile}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object AkkaPersistenceExtendedTask {
  private val PersId = "AkkaPersistenceExtendedTask"

  sealed trait Command

  case class Add(amount: Double) extends Command

  case class Multiply(amount: Double) extends Command

  case class Divide(amount: Double) extends Command

  sealed trait Event

  case class Added(id: String, amount: Double) extends Event

  case class Multiplied(id: String, amount: Double) extends Event

  case class Divided(id: String, amount: Double) extends Event

  trait CborSerialization

  object CalculatorWriteSide {
    final case class State(value: Double) extends CborSerialization {
      def add(amount: Double): State = copy(value = value + amount)

      def multiply(amount: Double): State = copy(value = value * amount)

      def divide(amount: Double): State = copy(value = value / amount)
    }

    def handleCommand(persistenceId: String,
                      state: State,
                      command: Command,
                      ctx: ActorContext[Command]): Effect[Event, State] =
      command match {
        case Add(amount) =>
          ctx.log.info(s"receive adding  for number: $amount and state is ${state.value}")
          val added = Added(persistenceId, amount)
          Effect
            .persist(added)
            .thenRun {
              x => ctx.log.info(s"The state result is ${x.value}")
            }
        case Multiply(amount) =>
          ctx.log.info(s"receive multiplying for number: $amount and state is ${state.value}")
          val multiplied = Multiplied(persistenceId, amount)
          Effect
            .persist(multiplied)
            .thenRun {
              x => ctx.log.info(s"The state result is ${x.value}")
            }
        case Divide(amount) =>
          ctx.log.info(s"receive dividing  for number: $amount and state is ${state.value}")
          val divided = Divided(persistenceId, amount)
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
        EventSourcedBehavior[Command, Event, State](
          PersistenceId.ofUniqueId(PersId),
          State(0),
          (state, command) => handleCommand(PersId, state, command, ctx),
          (state, event) => handleEvent(state, event, ctx)
        )
      }
  }

  case class CalculatorReadSide(system: ActorSystem[NotUsed]) {

    import CalculatorRepository._

    implicit val materializer: actor.ActorSystem = system.classicSystem

    var (latestCalculatedResult, offset) = Result.unapply(getLatestsOffsetAndResult).get
    val startOffset: Long = if (offset == 1) 1 else offset + 1

    val readJournal: CassandraReadJournal = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

    val source: Source[EventEnvelope, NotUsed] =
      readJournal.eventsByPersistenceId(PersId, startOffset, Long.MaxValue)

    def updateState(event: EventEnvelope): Result =
      event.event match {
        case Added(_, amount) =>
          println(s"Added($amount), prev val = $latestCalculatedResult")
          Result(latestCalculatedResult + amount, event.sequenceNr)
        case Multiplied(_, amount) =>
          println(s"Multiplied($amount), prev val = $latestCalculatedResult")
          Result(latestCalculatedResult * amount, event.sequenceNr)
        case Divided(_, amount) =>
          println(s"Divided($amount), prev val = $latestCalculatedResult")
          Result(latestCalculatedResult / amount, event.sequenceNr)
      }

    val graph: Graph[ClosedShape.type, NotUsed] = GraphDSL.create() {
      implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._
        import session.profile.api._

        val input = builder.add(source)
        val stateUpdater = builder.add(Flow[EventEnvelope].map(e => updateState(e)))
        val broadcast = builder.add(Broadcast[Result](2))
        val localSaveOutput = builder.add(Sink.foreach[Result] {
          r =>
            latestCalculatedResult = r.state
            println(s"New local state: $latestCalculatedResult")
        })
        val dbSaveOutput = builder.add({
          Slick.sink((r: Result) => {
            println(s"New db    state: ${r.state}")
            sqlu"update public.result set calculated_value = ${r.state}, write_side_offset = ${r.offset} where id = 1"
          })
        })

        input ~> stateUpdater
        stateUpdater ~> broadcast
        broadcast.out(0) ~> localSaveOutput
        broadcast.out(1) ~> dbSaveOutput

        ClosedShape
    }
  }

  object CalculatorRepository {
    val db = DatabaseConfig.forConfig[JdbcProfile]("slick-postgres")
    implicit val session: SlickSession = SlickSession.forConfig(db)

    case class Result(state: Double, offset: Long)

    def getLatestsOffsetAndResult: Result = {
      import session.profile.api._
      implicit val getUserResult: AnyRef with GetResult[Result] = GetResult(r => Result(r.nextDouble(), r.nextInt()))

      val select = session.db.run(sql"select calculated_value, write_side_offset from public.result where id = 1;".as[Result].headOption)

      val res = Await.result(select, 1 seconds)
      res match {
        case Some(v) => v
        case None => throw new RuntimeException("There is nol record")
      }
    }
  }

  def apply(): Behavior[NotUsed] =
    Behaviors.setup {
      ctx =>
        val writeAcorRef = ctx.spawn(CalculatorWriteSide(), "Calculator", Props.empty)
        writeAcorRef ! Add(10)
        writeAcorRef ! Multiply(2)
        writeAcorRef ! Divide(7)

        Behaviors.same
    }

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[NotUsed] = ActorSystem(AkkaPersistenceExtendedTask(), "AkkaPersistenceExtendedTask")
    val rs = CalculatorReadSide(system)
    RunnableGraph.fromGraph(rs.graph).run()
  }
}
