package com.llmchat

import com.llmchat.utils.{AppConfig, AppLogger}

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import scala.util.{Failure, Success}

object RESTServer {
  val logger = AppLogger("REST-Server")

  //#start-http-server
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext
    logger.info("Setting up HTTP server...")

    val futureBinding = Http().newServerAt("localhost", 8000).bind(routes)

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
    logger.info("Start loading config")
    val conf = AppConfig()
    logger.info("Load config success")

    logger.info("Set up server")
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val routes = new Routes()(context.system)
      startHttpServer(routes.route)(context.system)
      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "AkkaHttpServer")
    //#server-bootstrapping
  }
}
//#main-class
