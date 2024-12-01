package com.llmchat.utils

import org.slf4j.{Logger, LoggerFactory}

//case class AppLogger(logger: Logger)
//  def apply() : Logger = logger

object AppLogger {

  val RESET = "\u001B[0m"
  val RED = "\u001B[31m"
  val GREEN = "\u001B[32m"
  val YELLOW = "\u001B[33m"
  val BLUE = "\u001B[34m"
  val PURPLE = "\u001B[35m"
  val CYAN = "\u001B[36m"
  val WHITE = "\u001B[37m"
  
  def apply(name: String): Logger = {
    val logger = LoggerFactory.getLogger(name)
    Option(getClass.getClassLoader.getResourceAsStream("logback.xml")) match {
      case None => logger.error("Failed to locate logback.xml")
      case Some(inStream) =>
        try {
        } finally {
          inStream.close()
        }
    }
    logger
  }
}