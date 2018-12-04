package model

import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.util.ArrayData

class Spark {
  val conf = new SparkConf()
  conf.setMaster("local")
  conf.setAppName("StreetCred")
  val sc = new SparkContext(conf)
  val ss: SparkSession = SparkSession.builder()
    .master("local")
    .appName("StreetCred")
    .config("spark.mongodb.input.uri", "mongodb://127.0.0.1/StreetCred.Train")
    .config("spark.mongodb.output.uri", "mongodb://127.0.0.1/StreetCred.Train")
    .getOrCreate()
  val consumerToken = ConsumerToken(key = "T9H5bGk6mm6xFGsm0Plr3kMO7", secret = "z34jTfJzR3C30WuCSSjfG1MBKcsRv4h0a22dLHhLwsVgxEPBKN")
  val accessToken = AccessToken(key = "294518321-uByqtgwisRTvuYoUYoVcQjr965KrUFbwXbSI563B", secret = "kdqXnHuCeBJjvscsizntPej490YhDPF2lj6ORjVfBXhIq")
}
