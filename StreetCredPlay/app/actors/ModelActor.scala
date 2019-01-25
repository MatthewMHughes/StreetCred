package actors

import actors.ModelActor.displayCred
import actors.SearchActor.{getCreds, retrainModel}
import akka.actor.{Actor, ActorRef, Props}
import com.mongodb.spark.MongoSpark
import com.mongodb.spark.config.ReadConfig
import model._
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.StreamingContext
import model.Spark
import org.apache.spark.ml.PipelineModel
import org.apache.spark.ml.classification.DecisionTreeClassificationModel
import play.api.libs.json.JsValue

object ModelActor {
  def props(spark: Spark): Props = Props(new ModelActor(spark))
  case class displayCred(cred: Array[Double], tweets: List[String])
}

class ModelActor(spark: Spark) extends Actor{

  def receive: PartialFunction[Any, Unit] ={
    // Get credibibilities for a given list of tweets
    case getCreds(cred, tweets) =>
      val extractor = new FeatureExtractor(spark.sc, spark.ss, cred)
      val features = extractor.extractFeatures()
      val model = new Model(spark.sc, spark.ss, features)
      model.setModel()
      val predictions = model.getPredictions(features)
      sender() ! displayCred(predictions, tweets)
    // Retrain the model with updated training data
    case retrainModel() =>
      val readConfig = ReadConfig(Map("uri" -> "mongodb://127.0.0.1/", "database" -> "StreetCred", "collection" -> "Train"))
      var df = MongoSpark.load(spark.ss, readConfig)
      val extractor = new FeatureExtractor(spark.sc, spark.ss, df)
      val features = extractor.extractFeatures()
      val Array(train, test) = features.randomSplit(Array[Double](0.7, 0.3))
      val evalModel = new Model(spark.sc, spark.ss, train)
      evalModel.trainModel()
      val eval = new Evaluator(spark.sc, spark.ss, test, evalModel)
      eval.evaluateModel()
  }
}
