package model

import org.apache.spark.SparkContext
import org.apache.spark.ml.PipelineModel
import org.apache.spark.ml.classification.DecisionTreeClassificationModel
import org.apache.spark.ml.feature.CountVectorizerModel
import org.apache.spark.ml.linalg.SparseVector
import org.apache.spark.sql.SparkSession

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
// import classes required for using GraphX

class Node(val parent: Node){
  var left: Node = _
  var right: Node = _
  var feature: Double = _
  var value: Double = _
  var prediction: Double = -1
}

class Decision(val sc: SparkContext, val ss: SparkSession, cv: PipelineModel) {
  var root: Node = _
  var count: CountVectorizerModel = cv.stages(2).asInstanceOf[CountVectorizerModel]
  var features: Array[String] = count.vocabulary
  // Recreates the decision tree from the debug string
  def createTree(): Node = {
    val tree = cv.stages.last.asInstanceOf[DecisionTreeClassificationModel] // gets the string description of the tree
    val str = tree.toDebugString
    print(str)
    val trimmedList: List[String] = str.split("\n").map(_.trim).toList // splits each line which corresponds to a rule
    var id = 1
    root = new Node(null)
    var nextNode = root
    // for all rules
    for (line <- trimmedList) {
      id += 1
      // split line into the components
      val curLine = line.split(" +")
      // gets rid of first line of debug string which is just a title
      if (curLine(0).equals("DecisionTreeClassificationModel")) {
      }
      // if the first word is if, then the next rule will go to the left child
      else if (curLine(0).equals("If")) {
        nextNode.feature = curLine(2).toDouble
        val valSize = curLine(4).length
        nextNode.value = curLine(4).substring(0, valSize - 1).toDouble
        val node = new Node(nextNode)
        nextNode.left = node
        nextNode = node
      }
      // if the first word is an else, then the next rule will go to the right child
      else if (curLine(0).equals("Else")) {
        val node = new Node(nextNode)
        nextNode.right = node
        nextNode = node
      }
      else {
        // Otherwise we have a prediction and so reach a leaf node. Go back up the tree till we find a node with no right child.
        // This is so that the corresponding else rule can then be added to the tree for the next if.
        nextNode.prediction = curLine(1).toDouble
        nextNode = nextNode.parent
        while (nextNode.right != null && nextNode.parent != null) {
          nextNode = nextNode.parent
        }
      }
    }
    root
  }

  def explain(vec: SparseVector, numFeatures: Int): String ={
    // We convert the vector to a string and will traverse the tree until we hit a prediction
    // Printing out the decision the tree has taken to go to the next level of the tree
    val ar = vec.toArray
    val featureName = Array("character count", "word count", "The tweet contains a url, The tweet doesn't contain a url",
      "number of hashtags", "number of retweets", "number of favourites", "user's follower count", "user's friend count",
      "user's number of tweets", "The user's profile contains a url, The user's profile doesn't contain a url",
      "The user is verified, The user is not verified", "The user has edited their profile, The user's profile is default",
      "The user has edited their picture, The user's picture is default", "The tweet contains media, The tweet doesn't contain media",
      "user's description length")
    val featureType = Array("c", "c", "b", "c", "c", "c", "c", "c", "c", "b", "b", "b", "b", "b", "c")
    val listBuf = new ListBuffer[String]()
    var start = root
    // until we reach a prediction - ie we have gone through the whole tree
    while(start.prediction == -1){
      // value is equal to the element in the array equal to the feature of the node
      val value = ar(start.feature.asInstanceOf[Int])
    // if the if statement is a less than or equals
      if(start.value > value){
        // if the operator is <= and the value is less than or equal node's value
        // add a line to describe the decision the tree has made
        if(start.feature < 15){
          if(featureType(start.feature.asInstanceOf[Int]) == "b"){
            val split = featureName(start.feature.asInstanceOf[Int]).split(",")
            val str = split(1)
            listBuf+=str
          }
          else{
            val str = "The " + featureName(start.feature.asInstanceOf[Int]) + " is less than " + start.value.ceil.toInt
            listBuf+=str
          }
        }
        else{
          val str = "Occurrences of the word " + features(start.feature.asInstanceOf[Int] - 15) + " is less than " + start.value.ceil.toInt
          listBuf+=str
        }
        // go to the left node since this is true
        start = start.left
      }
      else{
        // add a line to describe he decision the tree has made
        if(start.feature < 15){
          if(featureType(start.feature.asInstanceOf[Int]) == "b"){
            val split = featureName(start.feature.asInstanceOf[Int]).split(",")
            val str = split(0)
            listBuf+=str
          }
          else{
            val str = "The " + featureName(start.feature.asInstanceOf[Int]) + " is bigger than " + start.value.floor.toInt
            listBuf+=str
          }
        }
        else{
          val str = "Occurrences of the word " + features(start.feature.asInstanceOf[Int] - 15) + " is bigger than " + start.value.floor.toInt
          listBuf+=str
        }
        // go to the right node since it's false
        start = start.right
      }
    }
    // get the list and convert it to a single string with newline characters
    val list = listBuf.toList
    list.mkString("\n")
  }
}
