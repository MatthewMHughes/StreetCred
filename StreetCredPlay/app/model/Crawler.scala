package model

import org.apache.spark.streaming.{Seconds, StreamingContext}
import com.danielasfregola.twitter4s.TwitterRestClient
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken, Tweet}
import org.apache.spark.SparkContext
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}
import scala.util.{Failure, Success}
import scala.collection.mutable.ListBuffer

class Crawler(val consumerToken: ConsumerToken, val accessToken: AccessToken) {
  def searchTweets(query: String): Future[Seq[Tweet]]={
    val consumerToken = ConsumerToken(key = "T9H5bGk6mm6xFGsm0Plr3kMO7", secret = "z34jTfJzR3C30WuCSSjfG1MBKcsRv4h0a22dLHhLwsVgxEPBKN")
    val accessToken = AccessToken(key = "294518321-uByqtgwisRTvuYoUYoVcQjr965KrUFbwXbSI563B", secret = "kdqXnHuCeBJjvscsizntPej490YhDPF2lj6ORjVfBXhIq")
    val restClient = TwitterRestClient(consumerToken, accessToken)
    restClient.searchTweet(query=query, count=15).flatMap{ratedData =>
    val result = ratedData.data
    val tweets = result.statuses
      Future(tweets.sortBy(_.created_at))
    }
  }

  def search(query: String): List[String]= {
    def sleep(time: Long): Unit = Thread.sleep(time)
    var test = new ListBuffer[String]()
    val tweets = searchTweets(query)
    tweets onComplete {
      case Success(statuses) => for (tweet <- tweets) for(t <- tweet){
        test+=t.text
      }
      case Failure(t) => println("An error has occurred: " + t.getMessage)
    }
    sleep(3000)
    test.toList
  }
}
