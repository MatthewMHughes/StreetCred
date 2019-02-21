package model

import org.apache.spark.SparkContext
import org.apache.spark.ml.PipelineModel
import org.apache.spark.ml.classification.DecisionTreeClassificationModel
import org.apache.spark.ml.linalg.SparseVector
import org.apache.spark.sql.SparkSession

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
// import classes required for using GraphX

class Node(val parent: Node){
  var left: Node = _
  var right: Node = _
  var feature: Double = _
  var op: String = _
  var value: Double = _
  var prediction: Double = -1
}

class Decision(val sc: SparkContext, val ss: SparkSession, cv: PipelineModel) {
  var root: Node = _
  def createTree(): Node = {
    val tree = cv.stages.last.asInstanceOf[DecisionTreeClassificationModel]
    val str = tree.toDebugString
    val trimmedList: List[String] = str.split("\n").map(_.trim).toList
    var id = 1
    root = new Node(null)
    var nextNode = root
    for (line <- trimmedList) {
      id += 1
      val curLine = line.split(" +")
      if (curLine(0).equals("DecisionTreeClassificationModel")) {
      }
      else if (curLine(0).equals("If")) {
        nextNode.feature = curLine(2).toDouble
        nextNode.op = curLine(3)
        val valSize = curLine(4).length
        nextNode.value = curLine(4).substring(0, valSize - 1).toDouble
        val node = new Node(nextNode)
        nextNode.left = node
        nextNode = node
      }
      else if (curLine(0).equals("Else")) {
        val node = new Node(nextNode)
        nextNode.right = node
        nextNode = node
      }
      else {
        nextNode.prediction = curLine(1).toDouble
        nextNode = nextNode.parent
        while (nextNode.right != null && nextNode.parent != null) {
          nextNode = nextNode.parent
        }
      }
    }
    root
  }

  def parse(node: Node, dir: String): Unit = {
    println(dir)
    if (node == null) {
      return
    }
    if (node.op == null) {
      println("prediction" + node.prediction)
    } else {
      println(node.feature + node.op + node.value)
    }
    parse(node.left, "left")
    parse(node.right, "right")
  }

  def parseTree(): Unit = {
    println(root.feature + root.op + root.value)
    parse(root.left, "left")
    parse(root.right, "right")
  }

  def printGivenLevel(root: Node, level: Int): Unit = {
    if (root == null) return
    if (level == 1) println(root.feature + root.op + root.value)
    else if (level > 1) {
      printGivenLevel(root.left, level - 1)
      printGivenLevel(root.right, level - 1)
    }
  }

  def printLevelOrder(root: Node): Unit = {
    // Base Case
    if (root == null)
      return

    // Create an empty queue for level order tarversal
    val q = new mutable.Queue[Node]()

    // Enqueue Root and initialize height
    q += root
    var x = true
    while (x) {

      // nodeCount (queue size) indicates number of nodes
      // at current level.
      var nodeCount = q.length
      if (nodeCount == 0)
        x = false

      // Dequeue all nodes of current level and Enqueue all
      // nodes of next level
      while (nodeCount > 0) {
        val node = q.dequeue()
        if (node.op == null) {
          print("prediction: " + node.prediction + "     ")
        }
        else {
          print(node.feature + node.op + node.value + "    ")
        }

        if (node.left != null)
          q+=node.left
        else if(node.prediction != 100.0){
          val dummy = new Node(node)
          dummy.prediction = 100.0
          q+=dummy
        }
        if (node.right != null)
          q+=node.right
        else if(node.prediction != 100.0){//if node is not a dummy node
          val dummy = new Node(node)
          dummy.prediction = 100.0
          q+=dummy
        }
        nodeCount = nodeCount - 1
      }
      System.out.println()
    }
  }

  def explain(vec: SparseVector, numFeatures: Int): String ={
    // We convert the vector to a string and will traverse the tree until we hit a prediction
    // Printing out the decision the tree has taken to go to the next level of the tree
    val ar = vec.toArray
    val featureName = Array("character count", "word count", "contains a url", "number of hashtags", "number of retweets", "number of favourites", "user's follower count", "user's friend count", "user's number of tweets", "user's profile contains url", "if user is verified", "user has changed profile", "user has changed picture", "tweet contain media", "user's description length")
    val listBuf = new ListBuffer[String]()
    var start = root
    // until we reach a prediction - ie we have gone through the whole tree
    while(start.prediction == -1){
      // value is equal to the element in the array equal to the feature of the node
      val value = ar(start.feature.asInstanceOf[Int])
      // if the if statement is a less than or equals
       if(start.op.equals("<=")){
         // check whether the if statement holds
         if(start.value > value){
           // if the operator is <= and the value is less than or equal node's value
           // add a line to describe the decision the tree has made
           if(start.feature < 16){
             val str = "feature " + featureName(start.feature.asInstanceOf[Int]) + " is less than or equal to " + start.value
             listBuf+=str
           }
           else{
             val str = "feature " + start.feature + " is less than or equal to " + start.value
             listBuf+=str
           }
           // go to the left node since this is true
           start = start.left
         }
         else{
           // add a line to describe he decision the tree has made
           if(start.feature < 16){
             val str = "feature " + featureName(start.feature.asInstanceOf[Int]) + " is bigger than " + start.value
             listBuf+=str
           }
           else{
             val str = "feature " + start.feature + " is bigger than " + start.value
             listBuf+=str
           }
           // go to the right node since it's false
           start = start.right
         }
       }
         // this is a mirror as the if statement above just operation is >=
       else if(start.op.equals(">=")){
         if(start.value < value){
           if(start.feature < 16){
             val str = "feature " + featureName(start.feature.asInstanceOf[Int]) + " is bigger than or equal to " + start.value
             listBuf+=str
           }
           else{
             val str = "feature " + start.feature + " is bigger than or equal to " + start.value
             listBuf+=str
           }
           start = start.left
         }
         else{
           if(start.feature < 16){
             val str = "feature " + featureName(start.feature.asInstanceOf[Int]) + " is less than " + start.value
             listBuf+=str
           }
           else{
             val str = "feature " + start.feature + " is less than " + start.value
             listBuf+=str
           }
           start = start.right
         }
       }
    }
    // get the list and convert it to a single string with newline characters
    val list = listBuf.toList
    list.mkString("\n")
  }
}
