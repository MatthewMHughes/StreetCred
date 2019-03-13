package actors

import akka.actor._
import akka.stream.Materializer
import model.{Crawler, Spark}
import org.apache.spark.sql.SparkSession
import play.api.libs.json.{JsNumber, JsObject, JsString, JsValue}

import scala.collection.mutable.ListBuffer

object AboutActor {
  def props(out: ActorRef, system: ActorSystem, mat: Materializer, spark: Spark): Props = {
    val msg: JsValue = JsObject(Seq(
      "messageType" -> JsString("init")
    ))
    out ! msg
    val crawler = new Crawler(spark.ss, spark.sc)
    Props(new AboutActor(out, system, mat, crawler, spark))
  }
}

class AboutActor(out: ActorRef, system: ActorSystem, mat: Materializer, crawler: Crawler, spark: Spark) extends Actor {

  def receive:PartialFunction[Any, Unit] = {
    case msg: JsValue =>
      val socketMessage = msg("messageType")
      if (socketMessage == JsString("getTrends")){
        val trends = crawler.getTopHashtags(msg("id").toString.toInt)
        var count = 0
        for (trend <- trends){
          if (count < 10){
            var vol = "<10000"
            val volume = trend.getTweetVolume
            if(volume != -1){
              vol = volume.toString
            }
            if(trend.getName.charAt(0) != '#'){
              val message: JsValue = JsObject(Seq(
                "messageType" -> JsString("displayTrend"),
                "trend" -> JsString(trend.getName),
                "volume" -> JsString(vol)
              ))
              out ! message
              count+=1
            }
          }
        }
      }
      else if(socketMessage == JsString("getLoc")){
        val locations = crawler.getLocations()
        for((k, v) <- locations){
          val message: JsValue= JsObject(Seq(
            "messageType" -> JsString("displayOption"),
            "id" -> JsString(k),
            "name" -> JsString(v)
          ))
          out ! message
        }
      }
  }
}