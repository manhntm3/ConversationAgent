package com.llmchat

//#user-routes-spec
//#test-top
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.Done
import akka.util.Timeout

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Future
import scala.concurrent.duration._

//#set-up
class RoutesSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
  //#test-top

  // the Akka HTTP route testkit does not yet support a typed actor system (https://github.com/akka/akka-http/issues/2036)
  // so we have to adapt for now
  lazy val testKit = ActorTestKit()
  implicit def typedSystem: ActorSystem[_] = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem =
    testKit.system.classicSystem

  // use the json formats to marshal and unmarshall objects in the test
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._
  //#set-up

  // Create a fake ChatServiceClient to mock gRPC calls
  class FakeChatServiceClient extends ChatServiceClient {
    def saySomething(request: PromptRequest): Future[PromptReply] = {
      Future.successful(PromptReply("mock response"))
    }

    override def close(): Future[akka.Done] = Future.successful(Done)
    override def closed: Future[akka.Done] = Future.successful(Done)
  }

  val timeout: Timeout = 3.seconds
  // Instantiate the Routes class with the fake client
  val mockClient = new FakeChatServiceClient
  lazy val routes = new Routes(mockClient, timeout).route

  //#actual-test
  "Routes" should {
    "return sucess if (GET /chat)" in {
      // note that there's no need for the host part in the uri:
      val request = HttpRequest(uri = "/chat")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`text/plain(UTF-8)`)
        entityAs[String] should ===("Success")
      }
    }
    //#actual-test

    //#testing-post
    "be able to handle POST /chat" in {
      val prompt = Prompt("Test prompt")
      val promptEntity = Marshal(prompt).to[MessageEntity].futureValue

      val request = Post("/chat").withEntity(promptEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`text/plain(UTF-8)`)
        entityAs[String] should ===("mock response")
      }
    }
    //#actual-test
  }
  //#actual-test

  //#set-up
}
//#set-up
//#user-routes-spec
