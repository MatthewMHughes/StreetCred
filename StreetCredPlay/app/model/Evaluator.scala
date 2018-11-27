package model

import org.apache.spark.SparkContext
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.sql.{DataFrame, SparkSession}

class Evaluator(val sc: SparkContext, val ss: SparkSession, val df: DataFrame, val theModel: Model) {

  //returns number of true positives
  def truePositive(p: DataFrame): Long ={
    p.filter(p("labelIndex")===1 && p("prediction")===1).count()
  }

  //returns the number of true negatives
  def trueNegative(p: DataFrame): Long ={
    p.filter(p("labelIndex")===0 && p("prediction")===0).count()
  }

  //returns the number of false positives
  def falsePositive(p: DataFrame): Long ={
    p.filter(p("labelIndex")===0 && p("prediction")===1).count()
  }

  //returns the number of false negatives
  def falseNegative(p: DataFrame): Long ={
    p.filter(p("labelIndex")===1 && p("prediction")===0).count()
  }

  //Creates and prints out evaluation metrics model after the test data has been fitted by the model
  def evaluateModel(): Unit ={
    val model = theModel.model
    val predictions = model.transform(df)

    val totalPred = predictions.count()
    val totalCorrect = predictions.filter(predictions("labelIndex") === predictions("prediction")).count()
    println("Accuracy of test data: %d", totalCorrect.toFloat/totalPred)

    val tp = truePositive(predictions)
    val fp = falsePositive(predictions)
    val fn = falseNegative(predictions)
    val tn = trueNegative(predictions)
    println("True Positive: %d     False Positive: %d", tp, fp)
    println("False Negative: %d    True Negative: %d", fn, tn)


    val binEval = new BinaryClassificationEvaluator()
      .setLabelCol("labelIndex")
    print(binEval.evaluate(predictions))
  }
}
