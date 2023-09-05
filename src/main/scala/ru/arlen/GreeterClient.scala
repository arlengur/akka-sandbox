package ru.arlen

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.grpc.GrpcClientSettings

import scala.concurrent.{ExecutionContextExecutor, Future}

object GreeterClient {
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

  implicit val system: ActorSystem[String] = ActorSystem[String](func_style.Echo(), "func_actor")
  implicit val ec: ExecutionContextExecutor = system.executionContext

  private val clientSettings: GrpcClientSettings =
    GrpcClientSettings.fromConfig(GreeterService.name)

  private val client = GreeterServiceClient(clientSettings)

  def sendMessage(message: String): Future[HelloReply] =
    client.sayHello(
      HelloRequest(message)
    )
}
