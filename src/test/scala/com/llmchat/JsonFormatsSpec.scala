package com.llmchat

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spray.json._
import JsonFormats._

class JsonFormatsSpec extends AnyFlatSpec with Matchers {

  "Prompt JsonFormat" should "serialize and deserialize a Prompt correctly" in {
    val originalPrompt = Prompt("Test prompt")
    val json = originalPrompt.toJson
    val deserializedPrompt = json.convertTo[Prompt]
    deserializedPrompt shouldEqual originalPrompt
  }

  "Conversation JsonFormat" should "serialize and deserialize a Conversation correctly" in {
    val originalConversation = Conversation(List(Prompt("Hi"), Prompt("Hello")))
    val json = originalConversation.toJson
    val deserializedConversation = json.convertTo[Conversation]
    deserializedConversation shouldEqual originalConversation
  }
}