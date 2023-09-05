package ru.arlen

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class MySpec() extends TestKit(ActorSystem("MySpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {
  "An echo actor" should {
    "send message" in {
      val echo: ActorRef = system.actorOf(TestActors.echoActorProps)
      echo ! "hello"
      expectMsg("hello")
    }
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}