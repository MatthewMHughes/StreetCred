package actors

import actors.ModelActor.displayCred
import actors.SearchActor.getCreds
import akka.actor.{Actor, ActorRef, Props}
import com.mongodb.spark.MongoSpark
import com.mongodb.spark.config.ReadConfig
import model._
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.StreamingContext
import model.Spark
import org.apache.spark.ml.PipelineModel
import play.api.libs.json.JsValue

object ModelActor {
  def props(spark: Spark): Props = Props(new ModelActor(spark))
  case class displayCred(cred: Double)
}

class ModelActor(spark: Spark) extends Actor{

  def receive: PartialFunction[Any, Unit] ={
    case getCreds(cred) =>
      val extractor = new FeatureExtractor(spark.sc, spark.ss, cred)
      val features = extractor.extractFeatures()
      val pipeline = PipelineModel.load("/home/matthew/Documents/StreetCred/StreetCredPlay/app/model/the-model")
      val model = new Model(spark.sc, spark.ss, features, pipeline)
      val predictions = model.getPredictions(features)
      for(pred <- predictions){
        println(pred)
        sender() ! displayCred(pred)
      }
  }
}
