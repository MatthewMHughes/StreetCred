package actors

import akka.actor._
import akka.stream.Materializer
import com.fasterxml.jackson.databind.node.ObjectNode
import play.libs.Json
import akka.japi.pf.ReceiveBuilder
import com.fasterxml.jackson.databind.JsonNode

object SearchActor {
  def props(out: ActorRef, system: ActorSystem, mat: Materializer) = Props(new SearchActor(out, system, mat))
}

class SearchActor(out: ActorRef, system: ActorSystem, mat: Materializer) extends Actor {
  def receive = {
    case msg: String =>
      out ! ("I received your message: " + msg)
  }
}