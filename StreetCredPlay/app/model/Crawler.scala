package model


import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.typedLit

import scala.collection.mutable.ListBuffer
import twitter4j.auth.AccessToken
import twitter4j.conf.ConfigurationBuilder
import twitter4j.{Query, QueryResult, TwitterFactory, TwitterObjectFactory}

import collection.JavaConversions._

class Crawler(ss: SparkSession) {
  var df: DataFrame = _
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
    df = ss.read.json(tweetList.toList.toDS)
    df = df.withColumn("label", typedLit("verified"))
    idList.toList
  }
}