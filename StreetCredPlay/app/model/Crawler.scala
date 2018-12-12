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
  var df: DataFrame = _ // The dataframe of tweets
  var tweetsJson: java.util.List[String] = _ // List of tweets in json format
  import ss.implicits._
  val cb = new ConfigurationBuilder()
  cb.setJSONStoreEnabled(true)
  val twitter = new TwitterFactory(cb.build()).getInstance() // Create new twitter factory
  // Authorising with your Twitter Application credentials
  twitter.setOAuthConsumer("T9H5bGk6mm6xFGsm0Plr3kMO7",
    "z34jTfJzR3C30WuCSSjfG1MBKcsRv4h0a22dLHhLwsVgxEPBKN")
  twitter.setOAuthAccessToken(new AccessToken(
    "294518321-uByqtgwisRTvuYoUYoVcQjr965KrUFbwXbSI563B",
    "kdqXnHuCeBJjvscsizntPej490YhDPF2lj6ORjVfBXhIq"))
  //Takes in the query and return the queryResult from twitter api
  def searchTweets(query: String): QueryResult={
    val theQuery = new Query(query) // create new query with page size 20 and tweets being in english
    theQuery.setCount(20)
    theQuery.setLang("en")
    twitter.search(theQuery) // Search twitter rest api for the query
  }

  // Takes in the query and returns a list of twitter ids
  def search(query: String): List[String]= {
    // To build a list of tweet ids
    var idList = new ListBuffer[String]()
    // To create a list of raw json for the tweets - can be easily converted to a dataframe
    var tweetList = new ListBuffer[String]()
    val tweets = searchTweets(query).getTweets
    // For each tweet, append both the lists
    for(tweet <- tweets){
      idList+= String.valueOf(tweet.getId)
      val json = TwitterObjectFactory.getRawJSON(tweet)
      tweetList+=json
    }
    tweetsJson = tweetList.toList
    // Get dataframe for the tweets so we can classify the tweets
    df = ss.read.json(tweetList.toList.toDS)
    df = df.withColumn("label", typedLit("verified"))
    // Return list of tweet ids to display the tweets on the screen
    idList.toList
  }

  def updateCred(tid: Int, cred: Double): Unit ={
    // Get twitter raw json for the tweet whose credibility is being updated
    val json = tweetsJson.get(tid)
    // Convert to dataframe
    var tweetDf = ss.read.json(Seq(json).toDS)
    // If credibility given was 0.0 ie unverified, set it's label to verified else do the opposite
    if(cred == 0.0){
      tweetDf = tweetDf.withColumn("label", typedLit("verified"))
    }
    else{
      tweetDf = tweetDf.withColumn("label", typedLit("unverified"))
    }
    // Write tweet to training data
    val writeConfig = WriteConfig(Map("uri" -> "mongodb://127.0.0.1/", "database" -> "StreetCred", "collection" -> "Train"))
    MongoSpark.save(tweetDf, writeConfig)
  }

  def getTopHashtags(): List[String]={
    val trend = twitter.trends()
    val trends = trend.getPlaceTrends(21125)
    val hashtags = trends.getTrends
    var trendBuff = new ListBuffer[String]()
    for (hashtag <- hashtags){
      trendBuff+=hashtag.getQuery
    }
    trendBuff.toList
  }

  def streamHashtags(): Unit={

  }
}
