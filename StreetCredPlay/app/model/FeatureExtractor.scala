package model

import org.apache.spark.SparkContext
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SparkSession}

class FeatureExtractor(val sc: SparkContext, val ss: SparkSession, var df: DataFrame) {

  import ss.implicits._
  /* This will take the training data and return a vector of features */
  /*
  Features Used:
  1.  tweet's character count
  2.  tweet's word count
  3.  tweet contains URL
  4.  hashtag count
  5.  tweet contains geolocation
  6.  retweet count
  7.  favourite count
  8. user's followers count
  9. user's friends count
  10. user's number of tweets
  11. user has URL
  12. user is verified
  13. whether user has changed from the default profile
  14. whether user has changed from the default picture
  15. tweet contains media
  16. description length
   */

  //returns new column which is the character count of the tweet
  def characterCount(inputDf: DataFrame): DataFrame ={
    inputDf.withColumn("character_count", length($"full_text"))
  }

  //returns new column which is the word count of the tweet
  def wordCount(inputDf: DataFrame): DataFrame ={
    inputDf.withColumn("word_count", size(split($"full_text", " ")))
  }

  def averageWordLength(inputDf: DataFrame): DataFrame = {
    inputDf.withColumn("average_word_length", $"character_count"/$"word_count")
  }

  def favouriteRetweetRatio(inputDf: DataFrame): DataFrame = {
    inputDf.withColumn("favourite_retweet_ratio", $"retweet_count".cast("float")/$"favorite_count".cast("float"))
  }

  def followingFollowersRatio(inputDf: DataFrame): DataFrame = {
    inputDf.withColumn("following_followers_ratio", $"user.followers_count".cast("float")/$"user.friends_count".cast("float"))
  }

  //returns 1.0 if the tweet contains a url, 0.0 otherwise
  def tweetContainsURL(inputDf: DataFrame): DataFrame = {
    inputDf.withColumn("contains_url", (size($"entities.urls")!==0).cast("float"))
  }

  //returns 1.0 if the user has a url, 0.0 otherwise
  def userHasURL(inputDf: DataFrame): DataFrame = {
    inputDf.withColumn("user_has_url", $"user.url".isNotNull.cast("float"))
  }

  //returns the number of hashtags of the tweet
  def hashtagCount(inputDf: DataFrame): DataFrame ={
    inputDf.withColumn("hashtag_count", size($"entities.hashtags"))
  }

  //returns 1.0 if the tweet has geotagged coordinates, 0.0 otherwise
  def hasGEO(inputDf: DataFrame): DataFrame ={
    inputDf.withColumn("has_geo", $"geo".isNotNull.cast("float"))
  }

  //returns 1.0 if the user is verified, 0.0 otherwise
  def userVerified(inputDf: DataFrame): DataFrame ={
    inputDf.withColumn("user_verified", ($"user.verified"===true).cast("float"))
  }

  //returns 1.0 if the user has updated their profile from the default, 0.0 otherwise
  def changedProfile(inputDf: DataFrame): DataFrame ={
    inputDf.withColumn("changed_profile", ($"user.default_profile" === false).cast("float"))
  }

  //returns 1.0 if the user has changed their picture from the default, 0.0 otherwise
  def changedPicture(inputDf: DataFrame): DataFrame ={
    inputDf.withColumn("changed_picture", ($"user.default_profile_image" === false).cast("float"))
  }

  def changedUserProfile(inputDf: DataFrame): DataFrame ={
    inputDf.withColumn("changed_user_profile", ($"user.default_profile" === false && $"user.default_profile_image" === false).cast("float"))
  }

  //returns 1.0 if the tweet has any media - an image, video etc, 0.0 otherwise
  def tweetContainsMedia(inputDf: DataFrame): DataFrame ={
    val columns = inputDf.columns
    if(columns.contains("entities.media")){
      inputDf.withColumn("contains_media", (size($"entities.media")!==0).cast("float"))
    }
    else{
      inputDf.withColumn("contains_media", typedLit(0.0))
    }
  }

  //returns the length of the user's description
  def descriptionLength(inputDf: DataFrame): DataFrame ={
    inputDf.withColumn("description_length", length($"user.description"))
  }

  // takes in training data, puts it through all feature extracting functions and compile a vector of features
  def extractFeatures(): DataFrame ={
    df = characterCount(df)// 1
    df = wordCount(df)// 2
    df = tweetContainsURL(df)// 3
    df = hashtagCount(df)// 4
    df = hasGEO(df)// 5
    df = userHasURL(df)// 11
    df = userVerified(df)// 12
    df = changedProfile(df)// 13
    df = changedPicture(df)// 14
    df = tweetContainsMedia(df)// 15
    df = descriptionLength(df)// 16
    df = averageWordLength(df)// new1
    df = favouriteRetweetRatio(df)// new2
    df = followingFollowersRatio(df)// new3
    df = changedUserProfile(df)// new4
    var featureDf = df.select(
      $"character_count",// 1
      $"word_count",// 2
      $"contains_url",// 3
      $"hashtag_count",// 4
      $"has_geo",// 5
      $"retweet_count".cast("float"),// 6
      $"favorite_count".cast("float"),// 7
      $"user.followers_count".cast("float"),// 8
      $"user.friends_count".cast("float"),// 9
      $"user.statuses_count".cast("float"),// 10
      $"user_has_url",// 11
      $"user_verified",// 12
      $"changed_profile",// 13
      $"changed_picture",// 14
      $"contains_media",// 15
      $"description_length",// 16
      $"average_word_length",// new1
      $"favourite_retweet_ratio",// new2
      $"following_followers_ratio",// new3
      $"changed_user_profile",// new4
      $"labelS",
      $"full_text")
    featureDf = featureDf.na.fill(0.0, Seq("favourite_retweet_ratio"))
    featureDf = featureDf.na.fill(0.0, Seq("average_word_length"))
    featureDf = featureDf.na.fill(0.0, Seq("following_followers_ratio"))
    featureDf
  }
}
