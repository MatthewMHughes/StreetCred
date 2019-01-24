package model

import org.apache.spark.SparkContext
import org.apache.spark.ml.classification.{DecisionTreeClassificationModel, DecisionTreeClassifier}
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.sql.{DataFrame, SparkSession}
import co.theasi.plotly
import util.Random

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

  def credAndUrl(p : DataFrame): Unit ={
    val c1 = p.filter(p("prediction")===1 && p("contains_url")===1.0).count()
    val c2 = p.filter(p("prediction")===1 && p("contains_url")===0.0).count()
    val c3 = p.filter(p("prediction")===0 && p("contains_url")===1.0).count()
    val c4 = p.filter(p("prediction")===0 && p("contains_url")===0.0).count()
    println("Tweets marked as credible and contains URL: %d", c1)
    println("Tweets marked as credible and doesn't contains URL: %d", c2)
    println("Tweets marked as uncredible and contains URL: %d", c3)
    println("Tweets marked as uncredible and doesn't contains URL: %d", c4)

    val fp = p.filter(p("labelIndex")===0 && p("prediction")===1)
    val c5 = fp.filter(p("contains_url")===1.0).count()
    val c6 = fp.count() - c5
    println("False positive tweets that contains URL: %d", c5)
    println("False positive tweets that doesn't contains URL: %d", c6)


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

    credAndUrl(predictions)

    val binEval = new BinaryClassificationEvaluator()
      .setLabelCol("labelIndex")
    println(binEval.evaluate(predictions))
    /*val treeModel = model.stages(6).asInstanceOf[DecisionTreeClassificationModel]
    print(treeModel.featureImportances)
    print(treeModel.toDebugString)*/
    predictions.write.json("/home/matthew/Documents/StreetCred/StreetCredPlay/app/model/resources/csv")
  }
}
