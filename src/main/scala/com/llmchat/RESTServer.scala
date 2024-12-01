package com.llmchat

import com.llmchat.utils.{AppConfig, AppLogger}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import scala.concurrent.duration._

import scala.util.{Failure, Success}

object RESTServer {
  val logger = AppLogger("REST-Server")
  val conf = AppConfig()

  //#start-http-server
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext
    logger.info("Setting up HTTP server...")

    // Bind to local 0.0.0.0
    val futureBinding = Http().newServerAt("0.0.0.0", conf.getInt("rest-server.port")).bind(routes)

    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        logger.info(
          "Server online at http://{}:{}/",
          address.getHostString,
          address.getPort
        )
      case Failure(ex) =>
        logger.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  //#start-http-server
  def main(args: Array[String]): Unit = {
    //#server-bootstrapping

//    if (args.length < 1) {
//      logger.error("Cannot find the input argument!")
//      return
//    }

    logger.info("Set up server")
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val clientSettings = GrpcClientSettings.fromConfig("ChatService")(context.system)
      val client = ChatServiceClient(clientSettings)(context.system)
      val timeout = Timeout.create(context.system.settings.config.getDuration("rest-server.routes.ask-timeout"))
      val routes = new Routes(client, timeout)(context.system)
      startHttpServer(routes.route)(context.system)
      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "AkkaHttpServer")
    //#server-bootstrapping
  }
}
//#main-class
