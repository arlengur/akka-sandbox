package ru.arlen

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object func_style {
  object Echo {
    def apply(): Behavior[String] = Behaviors.setup { ctx =>
      Behaviors.receiveMessage {
        msg =>
          ctx.log.info(s"func: $msg")
          Behaviors.same
      }
    }
  }
}

object GreeterServer {
  implicit val system: ActorSystem[String] = ActorSystem[String](func_style.Echo(), "func_actor")
  implicit val ec: ExecutionContextExecutor = system.executionContext

  val service: HttpRequest => Future[HttpResponse] = GreeterServiceHandler.withServerReflection(new GreeterServiceImpl())

  def startServer: Future[Http.ServerBinding] = Http().newServerAt("localhost", 8080).bind(service)

  startServer.onComplete {
    case Success(binding) =>
      val addr = binding.localAddress
      println(s"gRPC server bound to ${addr.getHostString}:${addr.getPort}")
    case Failure(error) =>
      println("Failed to bind gRPC point, terminating system", error)
      system.terminate()
  }

  def main(args: Array[String]): Unit = {
    startServer
    GreeterClient.sendMessage("Hello").onComplete {
      case Failure(_) => throw new Exception("oops")
      case Success(value) =>
        println(value)
    }
  }
}
