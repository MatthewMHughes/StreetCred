package model

import org.apache.spark.SparkContext
import org.apache.spark.ml.classification.{DecisionTreeClassificationModel, DecisionTreeClassifier}
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.sql.{DataFrame, SparkSession}
import co.theasi.plotly
import org.apache.spark.ml.PipelineModel
import org.apache.spark.ml.feature.HashingTF

import util.Random

class Evaluator(val sc: SparkContext, val ss: SparkSession, val df: DataFrame, val theModel: Model) {

  //returns number of true positives
  def truePositive(p: DataFrame): Long ={
    p.filter(p("label")===1 && p("prediction")===1).count()
  }

  //returns the number of true negatives
  def trueNegative(p: DataFrame): Long ={
    p.filter(p("label")===0 && p("prediction")===0).count()
  }

  //returns the number of false positives
  def falsePositive(p: DataFrame): Long ={
    p.filter(p("label")===0 && p("prediction")===1).count()
  }

  //returns the number of false negatives
  def falseNegative(p: DataFrame): Long ={
    p.filter(p("label")===1 && p("prediction")===0).count()
  }


  //Creates and prints out evaluation metrics model after the test data has been fitted by the model
  def evaluateModel(): Unit ={
    val model = theModel.cv
    val predictions = model.transform(df)
    for (metric <- model.avgMetrics){
      println("average AUC ROC: " + metric)
    }

    val totalPred = predictions.count()
    val totalCorrect = predictions.filter(predictions("label") === predictions("prediction")).count()
    println("Accuracy of test data: " + totalCorrect.toFloat/totalPred)

    val tp = truePositive(predictions)
    val fp = falsePositive(predictions)
    val fn = falseNegative(predictions)
    val tn = trueNegative(predictions)
    println("True Positive: " + tp + "     False Positive: " + fp)
    println("False Negative: " + fn + "    True Negative: " + tn)

    val binEval = new BinaryClassificationEvaluator()
      .setLabelCol("label")
    println(binEval.evaluate(predictions))
    println("")

    val precision = tp.toFloat / (tp + fp)
    val recall = tp.toFloat / (tp + fn)
    val f1 = 2 * (precision * recall) / (precision + recall)
    val f5 = (1 + (0.5 * 0.5)) * (precision * recall) / (0.5 * precision + recall)
    println("precision: " + precision)
    println("recall: " + recall)
    println("f1: " + f1)
    println("f5: " + f5)
    println("")
  }
}
