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
  case class getCreds(cred: DataFrame, tweets: List[String]) //message class

}

class SearchActor(out: ActorRef, system: ActorSystem, mat: Materializer, crawler: Crawler, model: ActorRef, spark: Spark) extends Actor {
  def receive:PartialFunction[Any, Unit] = {
    case msg: JsValue =>
      val tweets = crawler.search(msg("query").toString)
      var id = 0
      for(tweet <- tweets){
        val message: JsValue = JsObject(Seq(
          "messageType" -> JsString("displayTweet"),
          "status" -> JsString(tweet),
          "id" -> JsNumber(id)
        ))
        id+=1
        out ! message
      }
      model ! getCreds(crawler.df, tweets)
    case displayCred(cred, tweets) =>
      var id = 0
      for(pred <- cred){
        val message: JsValue = JsObject(Seq(
          "messageType" -> JsString("displayCred"),
          "status" -> JsNumber(pred),
          "id" -> JsNumber(id)
        ))
        id+=1
        out ! message
      }
  }
}