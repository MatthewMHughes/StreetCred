package model

import org.apache.spark.SparkContext
import org.apache.spark.ml.classification.{DecisionTreeClassifier, LinearSVC, LogisticRegression, NaiveBayes}
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.feature.{IDF, _}
import org.apache.spark.ml.linalg.SparseVector
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.tuning.{CrossValidator, CrossValidatorModel, ParamGridBuilder}
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.collection.mutable.ListBuffer

class Model(val sc: SparkContext, val ss: SparkSession, val df: DataFrame, val f: Int, val g: Int) {
  var cv: CrossValidatorModel = _
  /*This is a pipeline that takes in a dataframe of the features,
  calculate tf-idf of the tweets and then vectorizes the features with tfidf
  Then trains a classification model on the features*/
  def pipeline(): CrossValidator = {

    //tokenizes the text of the tweet
    val tokenizer = new Tokenizer()
      .setInputCol("full_text")
      .setOutputCol("words")

    //removes stopwords from the tokens
    val remover = new StopWordsRemover()
      .setInputCol(tokenizer.getOutputCol)
      .setOutputCol("filtered")

    /*
    //calculates term frequency of the words
    val tf = new HashingTF()
      .setInputCol(remover.getOutputCol)
      .setNumFeatures(2000)
      .setOutputCol("rawFeatures")

    //calculated the tf-idf of the words - final feature
    val idf = IDF.load("app/model/the-model/tfidf")
    */

    val count = CountVectorizerModel.load("app/model/count")

    //indexes the credibility labels to 0 or 1. 1 if verified, 0 otherwise.
    val labelIndexer = new StringIndexer()
      .setInputCol("labelS")
      .setOutputCol("label")

    var vectorAssembler = new VectorAssembler()
    f match {
      case 0 =>
        vectorAssembler = new VectorAssembler()
          .setInputCols(Array("character_count", "word_count", "contains_url", "hashtag_count", "retweet_count", "favorite_count", "followers_count", "friends_count", "statuses_count", "user_has_url", "user_verified", "changed_profile", "changed_picture", "contains_media", "description_length", count.getOutputCol))
          .setOutputCol("features")
      case 1 =>
        vectorAssembler = new VectorAssembler()
          .setInputCols(Array("character_count", "word_count", "hashtag_count", "retweet_count", "favorite_count", "followers_count", "friends_count", "statuses_count", "user_has_url", "user_verified", "changed_profile", "changed_picture", "contains_media", "description_length", count.getOutputCol))
          .setOutputCol("features")
      case 2 =>
        vectorAssembler = new VectorAssembler()
          .setInputCols(Array("character_count", "word_count", "contains_url", "hashtag_count", "retweet_count", "favorite_count", "followers_count", "friends_count", "statuses_count", "user_has_url", "user_verified", "changed_profile", "changed_picture", "contains_media", "description_length"))
          .setOutputCol("features")
      case 3 =>
        vectorAssembler = new VectorAssembler()
          .setInputCols(Array("contains_url", "followers_count", "friends_count", "statuses_count", "user_has_url", "user_verified", "changed_profile", "changed_picture", "description_length", count.getOutputCol))
          .setOutputCol("features")
      case 4 =>
        vectorAssembler = new VectorAssembler()
          .setInputCols(Array("character_count", "word_count", "contains_url", "hashtag_count", "retweet_count", "favorite_count", "user_has_url", "user_verified", "changed_profile", "changed_picture", "contains_media", "description_length", count.getOutputCol))
          .setOutputCol("features")
      case 5 =>
        vectorAssembler = new VectorAssembler()
          .setInputCols(Array("character_count", "word_count", "contains_url", "hashtag_count", "retweet_count", "favorite_count", "followers_count", "friends_count", "statuses_count", "contains_media", count.getOutputCol))
          .setOutputCol("features")
      case 6 =>
        vectorAssembler = new VectorAssembler()
          .setInputCols(Array("contains_url"))
          .setOutputCol("features")
      case 7 =>
        vectorAssembler = new VectorAssembler()
        .setInputCols(Array("contains_url", count.getOutputCol))
        .setOutputCol("features")
      case 8 =>
        vectorAssembler = new VectorAssembler()
          .setInputCols(Array("average_word_length", "contains_url", "hashtag_count", "favourite_retweet_ratio", "following_followers_ratio", "statuses_count", "user_has_url", "user_verified", "changed_user_profile", "contains_media", "description_length", count.getOutputCol))
          .setOutputCol("features")
      case 9 =>
        vectorAssembler = new VectorAssembler()
          .setInputCols(Array("character_count", "word_count", "contains_url", "hashtag_count", "retweet_count", "favorite_count", "followers_count", "friends_count", "statuses_count", "user_has_url", "user_verified", "changed_profile", "changed_picture", "contains_media", "description_length", "average_word_length", "favourite_retweet_ratio", "following_followers_ratio", "changed_user_profile" , count.getOutputCol))
          .setOutputCol("features")
    }

    val lr = new LogisticRegression()
      .setMaxIter(10)
      .setRegParam(0.001)
      .setLabelCol("label")

    val lsvc = new LinearSVC()
      .setMaxIter(10)
      .setRegParam(0.1)
      .setLabelCol("label")

    val nb = new NaiveBayes()
      .setLabelCol("label")

    val dc = new DecisionTreeClassifier()
      .setLabelCol("label")
      .setMaxDepth(5)
      .setMaxBins(64)

    //creates a pipeline of the model
    val pipeline = new Pipeline()
    g match {
      case 0 =>
        pipeline.setStages(Array(tokenizer, remover, count, labelIndexer, vectorAssembler, dc))
      case 1 =>
        pipeline.setStages(Array(tokenizer, remover, count, labelIndexer, vectorAssembler, lr))
      case 2 =>
        pipeline.setStages(Array(tokenizer, remover, count, labelIndexer, vectorAssembler, lsvc))
      case 3 =>
        pipeline.setStages(Array(tokenizer, remover, count, labelIndexer, vectorAssembler, nb))
    }
    val paramGrid = new ParamGridBuilder()
      .build()

    val cv = new CrossValidator()
      .setEstimator(pipeline)
      .setEvaluator(new BinaryClassificationEvaluator)
      .setEstimatorParamMaps(paramGrid)
      .setNumFolds(5)

    cv
  }

  /* This will create the classification model and save it over the existing model */
  def trainModel(): CrossValidatorModel = {
    cv = pipeline().fit(df)
    cv.write.overwrite().save("app/model/the-model")
    cv
  }

  /* This will set the model to the saved model */
  def setModel(): CrossValidatorModel = {
    cv = CrossValidatorModel.load("app/model/the-model")
    cv
  }

  /* This will get the prediction labels for given tweets and return an array of the labels */
  def getPredictions(predDf: DataFrame): Array[Double] = {
    val pred = cv.transform(predDf)
    pred.select("prediction").rdd.map(r => r(0).asInstanceOf[Double]).collect()
  }

  /* This will return explanation for each tweet to why its been given a specific label */
  def getExplanation(predDf: DataFrame): List[String] = {
    val bm = PipelineModel.load("app/model/the-model/bestModel")
    val pred = cv.transform(predDf)
    val vectors = pred.select("features").rdd.map(r => r(0).asInstanceOf[SparseVector]).collect()
    val dec = new Decision(sc, ss, bm)
    val root = dec.createTree()
    val buffer = new ListBuffer[String]()
    for(vector <- vectors){
      buffer+=dec.explain(vector, 15)
    }
    buffer.toList
  }
}
