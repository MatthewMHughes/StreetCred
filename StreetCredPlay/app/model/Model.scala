package model

import org.apache.spark.SparkContext
import org.apache.spark.ml.classification.{LinearSVC, LogisticRegression, NaiveBayes}
import org.apache.spark.ml.feature.{IDF, _}
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.sql.{DataFrame, SparkSession}

class Model(val sc: SparkContext, val ss: SparkSession, val df: DataFrame, var model: PipelineModel) {

  /*This is a pipeline that takes in a dataframe of the features,
  calculate tf-idf of the tweets and then vectorizes the features with tfidf
  Then trains a classification model on the features*/
  def pipeline(): Pipeline = {

    //tokenizes the text of the tweet
    val tokenizer = new Tokenizer()
      .setInputCol("full_text")
      .setOutputCol("words")

    //removes stopwords from the tokens
    val remover = new StopWordsRemover()
      .setInputCol(tokenizer.getOutputCol)
      .setOutputCol("filtered")

    //calculates term frequency of the words
    val tf = new HashingTF()
      .setNumFeatures(1000)
      .setInputCol(remover.getOutputCol)
      .setOutputCol("rawFeatures")

    //calculated the tf-idf of the words - final feature
    val idf = new IDF()
      .setInputCol(tf.getOutputCol)
      .setOutputCol("tfidf")

    //indexes the credibility labels to 0 or 1. 1 if verified, 0 otherwise.
    val labelIndexer = new StringIndexer()
      .setInputCol("label")
      .setOutputCol("labelIndex")

    val vectorAssembler = new VectorAssembler()
      .setInputCols(Array("character_count", "word_count", "contains_url", "hashtag_count", "has_geo", "retweet_count", "favorite_count", "followers_count", "friends_count", "statuses_count", "user_has_url", "user_verified", "changed_profile", "changed_picture", "contains_media", "description_length", idf.getOutputCol))
      .setOutputCol("features")

    val lr = new LogisticRegression()
      .setMaxIter(10)
      .setRegParam(0.001)
      .setLabelCol("labelIndex")

    val lsvc = new LinearSVC()
      .setMaxIter(10)
      .setRegParam(0.1)
      .setLabelCol("labelIndex")

    val nb = new NaiveBayes()
      .setLabelCol("labelIndex")

    //creates a pipeline of the model
    val pipeline = new Pipeline()
      .setStages(Array(tokenizer, remover, tf, idf, labelIndexer, vectorAssembler, lsvc))

    pipeline
  }

  /* This will create the classification model */
  def trainModel(): PipelineModel = {
    model = pipeline().fit(df)
    model.write.overwrite().save("/home/matthew/Documents/StreetCred/StreetCredPlay/app/model/the-model")
    model
  }

  def getModel(): PipelineModel = {
    PipelineModel.load("/home/matthew/Documents/StreetCred/StreetCredPlay/app/model/the-model")
  }

  def getPredictions(predDf: DataFrame): Array[Double] = {
    val pred = model.transform(predDf)
    pred.select("prediction").rdd.map(r => r(0).asInstanceOf[Double]).collect()
  }
}
