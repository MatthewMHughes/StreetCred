package actors

import akka.actor._
import akka.stream.Materializer
import model.{Crawler, Spark}
import org.apache.spark.sql.SparkSession
import play.api.libs.json.{JsNumber, JsObject, JsString, JsValue}

import scala.collection.mutable.ListBuffer

object HomeActor {
  def props(out: ActorRef, system: ActorSystem, mat: Materializer, spark: Spark): Props = {
    val msg: JsValue = JsObject(Seq(
      "messageType" -> JsString("init")
    ))
    out ! msg
    val crawler = new Crawler(spark.ss)
    Props(new HomeActor(out, system, mat, crawler, spark))
  }
}

class HomeActor(out: ActorRef, system: ActorSystem, mat: Materializer, crawler: Crawler, spark: Spark) extends Actor {

  def receive:PartialFunction[Any, Unit] = {
    case msg: JsValue =>
      val socketMessage = msg("messageType")
      if (socketMessage == JsString("getTrends")){
        val trends = crawler.getTopHashtags()
        var count = 0
        for (trend <- trends){
          if (count < 10){
            if(trend.getName.charAt(0) != '#'){
              val message: JsValue = JsObject(Seq(
                "messageType" -> JsString("displayTrend"),
                "trend" -> JsString(trend.getName),
                "volume" -> JsNumber(trend.getTweetVolume)
              ))
              out ! message
              count+=1
            }
          }
        }
      }
  }
}
