package actors

import akka.actor._
import akka.stream.Materializer
import com.fasterxml.jackson.databind.node.ObjectNode
import play.libs.Json
import akka.japi.pf.ReceiveBuilder
import com.fasterxml.jackson.databind.JsonNode
import model.Crawler
import play.api.libs.json.{JsObject, JsString, JsValue}

object SearchActor {
  def props(out: ActorRef, system: ActorSystem, mat: Materializer):Props = {
    val msg: JsValue = JsObject(Seq(
      "messageType" -> JsString("init")
    ))
    out ! msg
    val crawler = new Crawler()
    Props(new SearchActor(out, system, mat, crawler))}

}

class SearchActor(out: ActorRef, system: ActorSystem, mat: Materializer, crawler: Crawler) extends Actor {
  def receive:PartialFunction[Any, Unit] = {
    case msg: JsValue =>
      val tweets = crawler.search(msg("query").toString)
      for(tweet <- tweets){
        val message: JsValue = JsObject(Seq(
          "messageType" -> JsString("displayTweet"),
          "status" -> JsString(tweet)
        ))
        out ! message
      }
  }
}