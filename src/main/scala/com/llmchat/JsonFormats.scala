package com.llmchat

//#json-formats
import spray.json.RootJsonFormat
import spray.json.DefaultJsonProtocol

//#json-formats

case class Prompt(prompt : String)
case class Conversation(chat : List[Prompt])

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._
  
  implicit val promptJsonFormat: RootJsonFormat[Prompt] = jsonFormat1(Prompt.apply)
  implicit val conversationJsonFormat: RootJsonFormat[Conversation] = jsonFormat1(Conversation.apply)
}
//#json-formats
