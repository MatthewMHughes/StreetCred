package model


import java.time.LocalDateTime
import java.util.Date

import com.mongodb.spark.MongoSpark
import com.mongodb.spark.config.{ReadConfig, WriteConfig}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.typedLit
import play.api.libs.json.{JsString, JsValue}
import org.bson.Document

import scala.collection.mutable.ListBuffer
import twitter4j.auth.AccessToken
import twitter4j.conf.ConfigurationBuilder
import twitter4j._

import collection.JavaConversions._
import scala.collection.immutable.Range

class Crawler(ss: SparkSession, sc: SparkContext) {
  var df: DataFrame = _ // The dataframe of tweets
  var tweetsJson: java.util.List[String] = _ // List of tweets in json format
  var nextPage: Query = _
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
  def searchTweets(query: String, setting: JsValue): QueryResult={
    val theQuery = new Query(query) // create new query with page size 20 and tweets being in english
    if(setting.equals(JsString("top"))){
      theQuery.setResultType(Query.POPULAR)
    }
    else if(setting.equals(JsString("new"))){
      theQuery.setResultType(Query.RECENT)
    }
    theQuery.setCount(25)
    //theQuery.setLang("en")
    twitter.search(theQuery) // Search twitter rest api for the query
  }

  // Takes in the query and returns a list of twitter ids
  def search(query: String, setting: JsValue): List[String]= {
    // To build a list of tweet ids
    var idList = new ListBuffer[String]()
    var tweetList = new ListBuffer[String]()
    // To create a list of raw json for the tweets - can be easily converted to a dataframe
    if (nextPage == null){
      val querySet = searchTweets(query, setting)
      val tweets = querySet.getTweets
      nextPage = querySet.nextQuery()
      for(tweet <- tweets){
        if(!tweet.isRetweet){
          idList+= String.valueOf(tweet.getId)
          val json = TwitterObjectFactory.getRawJSON(tweet)
          tweetList+=json
        }
      }
    }
    else{
      val querySet = twitter.search(nextPage)
      if(!querySet.hasNext){
        idList += "No more"
        return idList.toList
      }
      nextPage = querySet.nextQuery()
      val tweets = querySet.getTweets
      for(tweet <- tweets){
        if(!tweet.isRetweet){
          idList+= String.valueOf(tweet.getId)
          val json = TwitterObjectFactory.getRawJSON(tweet)
          tweetList+=json
        }
      }
    }
    // For each tweet, append both the lists
    if(tweetsJson == null){
      tweetsJson = tweetList.toList
    }else{
      tweetsJson = List.concat(tweetsJson, tweetList.toList)
    }
    // Get dataframe for the tweets so we can classify the tweets
    df = ss.read.json(tweetsJson.toList.toDS)
    // labelS is string representation of a label
    df = df.withColumn("labelS", typedLit("verified"))
    // Return list of tweet ids to display the tweets on the screen
    idList.toList
  }

  def updateCred(tid: Int, cred: Double, change: Boolean): Unit ={
    // Get twitter raw json for the tweet whose credibility is being updated
    val json = tweetsJson.get(tid)
    // Convert to dataframe
    var tweetDf = ss.read.json(Seq(json).toDS)
    // If the credibility is different to given label, reverse labels
    if(change) {
      // if original cred is 1.0 (verified), then set it to be unverified and vice versa
      if (cred == 1.0) {
        tweetDf = tweetDf.withColumn("labelS", typedLit("unverified"))
      }
      else {
        tweetDf = tweetDf.withColumn("labelS", typedLit("verified"))
      }
    }
      // Else, the credibility label given was correct and we store it
    else{
      if(cred == 1.0){
        tweetDf = tweetDf.withColumn("labelS", typedLit("verified"))
      }
      else{
        tweetDf = tweetDf.withColumn("labelS", typedLit("unverified"))
      }
    }
    // Write tweet to training data
    val writeConfig = WriteConfig(Map("uri" -> "mongodb://127.0.0.1/", "database" -> "StreetCred", "collection" -> "Train"))
    MongoSpark.save(tweetDf, writeConfig)
  }

  // returns top hashtags for specific area
  def getTopHashtags(id: Int): Array[Trend]={
    val trend = twitter.trends()
    val trends = trend.getPlaceTrends(id)
    val hashtags = trends.getTrends
    hashtags
  }

  def getLocations(): Map[String, String]={
    val readConfig = ReadConfig(Map("uri" -> "mongodb://127.0.0.1/", "database" -> "StreetCred", "collection" -> "Locations"))
    val df = MongoSpark.load(ss, readConfig)
    val name = df.select("name").rdd.map(r => r(0).asInstanceOf[String]).collect()
    val id = df.select("id").rdd.map(r => r(0).asInstanceOf[String]).collect()
    var map = new scala.collection.immutable.HashMap[String, String]
    var i = 0
    for (i <- 0 until name.length){
      map += (id(i) -> name(i))
    }
    map
  }

  def addSearch(query: String): Unit = {
    val writeConfig = WriteConfig(Map("uri" -> "mongodb://127.0.0.1/", "database" -> "StreetCred", "collection" -> "Searches"))
    var map = new scala.collection.immutable.HashMap[String, String]
    map += ("query" -> query)
    map += ("searches" -> "1")
    map += ("credible" -> "0")
    map += ("uncredible" -> "0")
    map += ("recent" -> LocalDateTime.now().toString)
    val rdd = sc.parallelize(Seq(new Document(map)))
    MongoSpark.save(rdd, writeConfig)
  }
}
