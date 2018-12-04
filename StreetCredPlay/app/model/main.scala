/*package model

import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken}
import com.mongodb.spark.MongoSpark
import com.mongodb.spark.config.ReadConfig
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.streaming.{Seconds, StreamingContext}

object main {

  def main(args: Array[String]) {

    //Create a SparkContext to initialize Spark
    val conf = new SparkConf()
    conf.setMaster("local")
    conf.setAppName("StreetCred")
    val sc = new SparkContext(conf)

    val ss = SparkSession.builder()
      .master("local")
      .appName("StreetCred")
      .config("spark.mongodb.input.uri", "mongodb://127.0.0.1/StreetCred.Train")
      .config("spark.mongodb.output.uri", "mongodb://127.0.0.1/StreetCred.Train")
      .getOrCreate()
    val ssc = StreamingContext()
    //Reads in training data from mongodb database
    val readConfig = ReadConfig(Map("uri" -> "mongodb://127.0.0.1/", "database" -> "StreetCred", "collection" -> "Train"))
    var df = MongoSpark.load(ss, readConfig)
    val extractor = new FeatureExtractor(sc, ss, df)
    val features = extractor.extractFeatures()

    val Array(train, test) = features.randomSplit(Array[Double](0.7, 0.3))
    val modeller = new Model(sc, ss, train, null)
    val model = modeller.trainModel()

    val evaluator = new Evaluator(sc, ss, test, modeller)
    evaluator.evaluateModel()

    val consumerToken = ConsumerToken(key = "my-consumer-key", secret = "my-consumer-secret")
    val accessToken = AccessToken(key = "my-access-key", secret = "my-access-secret")
    val crawler = new Crawler()
    val tweets = crawler.search("Glasgow")
  }
}
*/