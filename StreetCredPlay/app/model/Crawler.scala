package model


import com.mongodb.spark.MongoSpark
import com.mongodb.spark.config.{ReadConfig, WriteConfig}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.typedLit

import scala.collection.mutable.ListBuffer
import twitter4j.auth.AccessToken
import twitter4j.conf.ConfigurationBuilder
import twitter4j._

import collection.JavaConversions._

class Crawler(ss: SparkSession) {
  var df: DataFrame = _
  var tweetsJson: java.util.List[String] = _
  import ss.implicits._
  def searchTweets(query: String): QueryResult={
    val cb = new ConfigurationBuilder()
    cb.setJSONStoreEnabled(true)
    val twitter = new TwitterFactory(cb.build()).getInstance()
    // Authorising with your Twitter Application credentials
    twitter.setOAuthConsumer("T9H5bGk6mm6xFGsm0Plr3kMO7",
      "z34jTfJzR3C30WuCSSjfG1MBKcsRv4h0a22dLHhLwsVgxEPBKN")
    twitter.setOAuthAccessToken(new AccessToken(
      "294518321-uByqtgwisRTvuYoUYoVcQjr965KrUFbwXbSI563B",
      "kdqXnHuCeBJjvscsizntPej490YhDPF2lj6ORjVfBXhIq"))
    val theQuery = new Query(query)
    theQuery.setCount(20)
    theQuery.setLang("en")
    twitter.search(theQuery)
  }

  def search(query: String): List[String]= {
    var idList = new ListBuffer[String]()
    var tweetList = new ListBuffer[String]()
    val tweets = searchTweets(query).getTweets
    for(tweet <- tweets){
      idList+= String.valueOf(tweet.getId)
      val json = TwitterObjectFactory.getRawJSON(tweet)
      tweetList+=json
    }
    tweetsJson = tweetList.toList
    df = ss.read.json(tweetList.toList.toDS)
    df = df.withColumn("label", typedLit("verified"))
    idList.toList
  }

  def updateCred(tid: Int, cred: Double): Unit ={
    val json = tweetsJson.get(tid)
    var tweetDf = ss.read.json(Seq(json).toDS)
    if(cred == 0.0){
      tweetDf = tweetDf.withColumn("label", typedLit("verified"))
    }
    else{
      tweetDf = tweetDf.withColumn("label", typedLit("unverified"))
    }
    val writeConfig = WriteConfig(Map("uri" -> "mongodb://127.0.0.1/", "database" -> "StreetCred", "collection" -> "Test"))
    MongoSpark.save(tweetDf, writeConfig)
  }
}