package com.llmchat.utils

import com.typesafe.config.{Config, ConfigFactory}

import java.util

object AppConfig {
  val conf: Config = ConfigFactory.load()
  def apply() : Config = conf
}
