package com.llmchat

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.model.headers.`Content-Type`
import akka.http.scaladsl.model.ContentTypes
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.{verify, when}
import org.mockito.ArgumentMatchers.any
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import io.github.ollama4j.OllamaAPI
import io.github.ollama4j.models.response.OllamaResult

class ConversationalAgentSpec extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar {
  implicit val testKit: ActorTestKit = ActorTestKit()
  implicit val system: ActorSystem[_] = testKit.system
  implicit val ec: ExecutionContext = system.executionContext

  "ConversationalAgent" should {
    "process conversation correctly" in {
      // Mock OllamaAPI
      val mockOllamaAPI = mock[OllamaAPI]
      when(mockOllamaAPI.generate(any, any, any, any)).thenReturn(
        new OllamaResult("Mocked Ollama Response", 1, 200)
      )

      // Mock forwardToServerFunc
      def mockForwardToServer(prompt: Prompt): Future[HttpResponse] = {
        val mockResponseJson =
          """
            |{
            |  "body": "{\"answer\": \"Mocked Server Response\"}"
            |}
          """.stripMargin

        Future.successful(
          HttpResponse(
            status = StatusCodes.OK,
            entity = HttpEntity(ContentTypes.`application/json`, mockResponseJson)
          )
        )
      }

      val initialPrompt = Prompt("Initial Test Prompt")

      // Run the conversation with mocks
      val conversationFuture = ConversationalAgent.startConversation(
        initialPrompt,
        maxIterations = 1,
        ollamaAPI = mockOllamaAPI,
        forwardToServerFunc = mockForwardToServer
      )

      whenReady(conversationFuture, timeout(5.seconds)) { _ =>
        // Verify interactions
        verify(mockOllamaAPI).generate(any, any, any, any)
      }
    }
  }

  "ConversationalAgent" should {
    "handle errors from OllamaAPI gracefully (will show error above)" in {
      // Mock OllamaAPI to throw an exception
      val mockOllamaAPI = mock[OllamaAPI]
      when(mockOllamaAPI.generate(any, any, any, any)).thenThrow(new RuntimeException("Ollama Error"))

      // Mock forwardToServerFunc
      def mockForwardToServer(prompt: Prompt): Future[HttpResponse] = {
        val mockResponseJson =
          """
            |{
            |  "body": "{\"answer\": \"Mocked Server Response\"}"
            |}
            """.stripMargin

        Future.successful(
          HttpResponse(
            status = StatusCodes.OK,
            entity = HttpEntity(ContentTypes.`application/json`, mockResponseJson)
          )
        )
      }

      val initialPrompt = Prompt("Initial Test Prompt")

      val conversationFuture = ConversationalAgent.startConversation(
        initialPrompt,
        maxIterations = 1,
        ollamaAPI = mockOllamaAPI,
        forwardToServerFunc = mockForwardToServer
      )

      whenReady(conversationFuture, timeout(5.seconds)) { _ =>
        // Verify that the exception was handled and the conversation terminated
        verify(mockOllamaAPI).generate(any, any, any, any)
      }
    }
  }

  // Clean up the ActorTestKit after tests
  def afterAll(): Unit = {
    testKit.shutdownTestKit()
  }
}