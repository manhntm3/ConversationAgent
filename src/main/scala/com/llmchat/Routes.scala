package com.llmchat

import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.Future
import akka.actor.typed.ActorSystem
import akka.util.Timeout
import akka.grpc.GrpcClientSettings
import com.llmchat.utils.AppLogger

import scala.util.Failure
import scala.util.Success

//#import-json-formats
//#user-routes-class
class Routes()(implicit val system: ActorSystem[_]) {
  val logger = AppLogger("Routes")

  //#user-routes-class
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._

  private def forwardToGrpcApiGateway(prompt: Prompt): Future[PromptReply] = {
    val client = ChatServiceClient(GrpcClientSettings.fromConfig("ChatService"))
    logger.info(s"Performing request: $prompt")
    val request = PromptRequest(prompt.prompt)
    client.saySomething(request)
  }
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("rest-server.routes.ask-timeout"))

  //#all-routes
  //#users-get-post
  //#users-get-delete
  val route: Route =
    pathPrefix("chat") {
      pathEnd {
        concat (
          get {
            complete("Success")
          },
          post {
            entity(as[Prompt]) { prompt =>
              // Forward the prompt to the API Gateway
              logger.warn(s"New prompt request ")
              val responseFuture = forwardToGrpcApiGateway(prompt)
              onComplete(responseFuture) {
                case scala.util.Success(response) =>
                  logger.info(s"Received response: $response")
                  complete(response.message) // Return the API Gateway response entirely
                case scala.util.Failure(ex) =>
                  complete(StatusCodes.InternalServerError, s"Error: ${ex.getMessage}")
              }
            }
          }
        )
      }
    }
  //#all-routes
}
