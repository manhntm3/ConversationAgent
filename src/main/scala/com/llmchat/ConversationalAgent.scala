package com.llmchat

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.unmarshalling.Unmarshal
import io.github.ollama4j.OllamaAPI
import io.github.ollama4j.models.response.OllamaResult
import io.github.ollama4j.utils.Options
import io.github.ollama4j.utils.OptionsBuilder

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.Random
import com.llmchat.utils.{AppConfig, AppLogger}
import com.llmchat.JsonFormats.promptJsonFormat
import com.typesafe.config.Config
import org.slf4j.Logger
import io.circe._, io.circe.parser._

import scala.util.Failure
import scala.util.Success

object ConversationalAgent {
  val logger: Logger = AppLogger("ConversationalAgent")

  val conf: Config = AppConfig()
  private val ollamaConf = conf.getConfig("ollama")
  private val serverConf = conf.getConfig("rest-server")


  val guideStrings = List(
    "How can you respond to the statement: ",
    "Do you have any comments on: ",
    "Your response to this: ",
    "What is the best way to address: "
  )

  private def processWithOllama(ollamaAPI : OllamaAPI, input: String)(implicit ec: ExecutionContext): Future[String] = Future {
    val randomGuide = guideStrings(Random.nextInt(guideStrings.length))
    val generateNextQueryPrompt = randomGuide + input
    try {
      val result: OllamaResult = ollamaAPI.generate(
        ollamaConf.getString("model"),
        generateNextQueryPrompt,
        false,
        new OptionsBuilder()
          .setMirostatEta(ollamaConf.getDouble("mirostat_eta").toFloat)
          .setNumPredict(ollamaConf.getInt("num_predict"))
          .build()
      )
      logger.debug(s"INPUT to Ollama: $generateNextQueryPrompt")
      logger.debug(s"Ollama OUTPUT: ${result.getResponse}")
      result.getResponse
    } catch {
      case e: Exception =>
        logger.error("Ollama processing failed", e)
        throw e
    }
  }
  
  private def forwardToServer(prompt: Prompt)(implicit sys: ActorSystem[_]): Future[HttpResponse] = {
    import sys.executionContext
    
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
    import spray.json._
    import akka.http.scaladsl.model._
    import akka.http.scaladsl.Http
    import akka.http.scaladsl.model.HttpMethods

    val promptJson = prompt.toJson.compactPrint
    val requestEntity = HttpEntity(ContentTypes.`application/json`, promptJson)
    val serverUrl = serverConf.getString("address")
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = serverUrl,
      entity = requestEntity
    )

    Http()(sys).singleRequest(request)
  }

  private def startConversation(initialPrompt: Prompt, maxIterations: Int = 10): Unit = {
    implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](Behaviors.empty[Nothing], "ConversationalAgent")
    implicit val ec: ExecutionContextExecutor = system.executionContext
    
    val ollamaAPI: OllamaAPI = new OllamaAPI(ollamaConf.getString("host"))
    ollamaAPI.setVerbose(false)
    ollamaAPI.setRequestTimeoutSeconds(ollamaConf.getInt("request-timeout-seconds"))
    
    def loop(prompt: Prompt, iteration: Int): Unit = {
      if (iteration > maxIterations) {
        logger.warn("Reached maximum iterations. Conversation ended.")
        system.terminate()
      } else {
        logger.debug(s"Iteration $iteration: Sending prompt to server.")
        val promptLog = prompt.prompt.replaceAll("\n", s"\n${AppLogger.YELLOW}")
        logger.info(AppLogger.YELLOW + s" User1: ${promptLog}" + AppLogger.RESET)

        val responseFuture: Future[String] = for {
          httpResponse <- forwardToServer(prompt)(system)
          responseString <- Unmarshal(httpResponse.entity).to[String]
        } yield responseString

        responseFuture.onComplete {
          case Success(serverResponse) =>
            logger.debug(s"Received response from server: $serverResponse")

            // Parse the outer JSON
            val parsedOuterJson = parse(serverResponse).getOrElse(Json.Null)

            // Extract the `body` field as a JSON string
            val bodyString = parsedOuterJson.hcursor.downField("body").as[String].getOrElse("")

            // Parse the inner JSON (body)
            val parsedInnerJson = parse(bodyString).getOrElse(Json.Null)

            val generationContent = parsedInnerJson.hcursor.downField("answer").as[String].getOrElse("")

            val generationContentLog = generationContent.replaceAll("\n", s"\n${AppLogger.GREEN}")
            logger.info(AppLogger.GREEN + s"User2: $generationContentLog" + AppLogger.RESET)

            val ollamaFuture = processWithOllama(ollamaAPI, generationContent)(ec)

            ollamaFuture.onComplete {
              case Success(ollamaResponse) =>
                logger.debug(s"Ollama processed response: $ollamaResponse")
                val nextPrompt = Prompt(ollamaResponse)
                loop(nextPrompt, iteration + 1)

              case Failure(ex) =>
                logger.error(s"Ollama processing failed: ${ex.getMessage}")
                system.terminate()
            }

          case Failure(ex) =>
            logger.error(s"Failed to get response from server: ${ex.getMessage}")
            system.terminate()
        }
      }
    }

    loop(initialPrompt, 1)
  }

  def main(args: Array[String]): Unit = {

    val beginPrompt = Prompt(args.mkString(" "))
    startConversation(beginPrompt)
  }
}