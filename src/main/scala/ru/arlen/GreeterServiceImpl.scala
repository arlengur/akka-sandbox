package ru.arlen

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import play.api.libs.json._

import scala.concurrent.Future


case class JsonObj(data: String)

class GreeterServiceImpl(implicit val mat: Materializer) extends GreeterService {
  val res: JsObject = JsObject(Seq("data" -> JsString("good value")))
  override def sayHello(in: HelloRequest): Future[HelloReply] = Future.successful(HelloReply(res.toString()))

  override def sayHelloToAll(in: Source[HelloRequest, NotUsed]): Source[HelloReply, NotUsed] = ???
}
