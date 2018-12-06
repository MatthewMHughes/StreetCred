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
    //handshake connection with frontend
    val msg: JsValue = JsObject(Seq(
      "messageType" -> JsString("init")
    ))
    out ! msg
    //create a new twitter crawler to search for tweets
    val crawler = new Crawler(spark.ss)
    Props(new SearchActor(out, system, mat, crawler, model, spark))}
  case class getCreds(cred: DataFrame, tweets: List[String]) //message class

}

class SearchActor(out: ActorRef, system: ActorSystem, mat: Materializer, crawler: Crawler, model: ActorRef, spark: Spark) extends Actor {
  def receive:PartialFunction[Any, Unit] = {
    // If its a message from the frontend websocket we receive a JSValue
    case msg: JsValue =>
      val socketMessage = msg("messageType")
      // If the message is "doSearch"
      if(socketMessage == JsString("doSearch")){
        // Query twitter crawler to get tweets for query given by user
        val tweets = crawler.search(msg("query").toString)
        // Tweet number on page
        var id = 0
        for(tweet <- tweets){
          // Create and send message to frontend with twitter id and tweet number
          val message: JsValue = JsObject(Seq(
            "messageType" -> JsString("displayTweet"),
            "status" -> JsString(tweet),
            "id" -> JsNumber(id)
          ))
          id+=1
          out ! message
        }
        // Send getCreds message to model actor to get credibility of the tweets
        model ! getCreds(crawler.df, tweets)
      }
        // If the message is "updateCred"
      else if(socketMessage == JsString("updateCred")){
        val tid = msg("id").toString.toInt
        val cred = msg("cred").toString.toDouble
        // Store the tweet in the database's training data
        crawler.updateCred(tid, cred)
      }
      // If the message is a displayCred class
    case displayCred(cred, tweets) =>
      // Tweet number on page
      var id = 0
      // For each prediction send it to the frontend to display next to corresponding tweet
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