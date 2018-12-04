package actors

import actors.ModelActor.displayCred
import actors.SearchActor.getCreds
import akka.actor._
import akka.stream.Materializer
import com.fasterxml.jackson.databind.node.ObjectNode
import play.libs.Json
import akka.japi.pf.ReceiveBuilder
import com.danielasfregola.twitter4s.entities.Tweet
import com.fasterxml.jackson.databind.JsonNode
import model.{Crawler, Spark}
import org.apache.spark.sql.DataFrame
import play.api.libs.json.{JsNumber, JsObject, JsString, JsValue}

object SearchActor {
  def props(out: ActorRef, system: ActorSystem, mat: Materializer, model: ActorRef, spark: Spark):Props = {
    val msg: JsValue = JsObject(Seq(
      "messageType" -> JsString("init")
    ))
    out ! msg
    val crawler = new Crawler(spark.ss)
    Props(new SearchActor(out, system, mat, crawler, model, spark))}
  case class getCreds(cred: DataFrame) //message class

}

class SearchActor(out: ActorRef, system: ActorSystem, mat: Materializer, crawler: Crawler, model: ActorRef, spark: Spark) extends Actor {
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
      model ! getCreds(crawler.df)
    case displayCred(cred) =>
      val message: JsValue = JsObject(Seq(
        "messageType" -> JsString("displayCred"),
        "status" -> JsNumber(cred)
      ))
      out ! message
  }
}