from pyspark.context import SparkContext
import time
from pyspark.sql import *
from pyspark.ml.feature import *
import pandas
from pyspark.mllib.regression import LabeledPoint
import csv
import numpy as np

sc = SparkContext("local", "Titanic")
sqlContext = SQLContext(sc)

#Read train.csv file and convert it to a dataframe
train = sqlContext.read.format("csv").option("header", "true").load("train.csv")
test = sqlContext.read.format("csv").option("header", "true").load("test.csv")
train = train.fillna({'Age':0})
test = test.fillna({'Age':0})
train = train.fillna({'Fare':0})
test = test.fillna({'Fare':0})

#This will convert Strings to an index. in this case Male to 0.0 and Female to 1.0
sexIndexer = StringIndexer(inputCol="Sex", outputCol="sexIndex")
sexLabeller = sexIndexer.fit(train)
trainSexIndexed = sexLabeller.transform(train)
testSexIndexed = sexLabeller.transform(test)

#This will take the data with new column with indexed sex and add a new column for an indexed embarked feature
EmbarkedIndexer = StringIndexer(inputCol="Embarked", outputCol="embarkedIndex", handleInvalid='keep')
embarkedLabeller = EmbarkedIndexer.fit(trainSexIndexed)
trainIndexed = embarkedLabeller.transform(trainSexIndexed)
testIndexed = embarkedLabeller.transform(testSexIndexed)

#Converts all columns that are strings into floats or ints so it can be vectorized
trainVector = trainIndexed.select(
    trainIndexed.PassengerId,
    trainIndexed.Pclass.cast("float"),
    trainIndexed.Age.cast("float"),
    trainIndexed.SibSp.cast("float"),
    trainIndexed.Parch.cast("float"),
    trainIndexed.Fare.cast("float"),
    trainIndexed.sexIndex,
    trainIndexed.embarkedIndex,
    trainIndexed.Survived.cast("float")
    )

from pyspark.ml.linalg import Vectors
from pyspark.ml.feature import VectorAssembler

#This converts our feature columns into a feature vector to be used for trainingf
assembler = VectorAssembler(
    inputCols=["Pclass", "Age", "SibSp", "Parch", "Fare", "sexIndex", "embarkedIndex"],
    outputCol="features")

training = assembler.transform(trainVector)

#Splits into training and testing data
(train_cv, test_cv) = training.randomSplit([0.7, 0.3])

#This will create a logistic regression model of our data
from pyspark.ml.classification import LogisticRegression
from pyspark.mllib.evaluation import BinaryClassificationMetrics
from pyspark.mllib.regression import LabeledPoint
lr = LogisticRegression(featuresCol = 'features', labelCol = 'Survived', maxIter=10)
lrModel = lr.fit(train_cv)

#This will predict on our test using lr model
predictions = lrModel.transform(test_cv)
predictions.select('PassengerId', 'Survived', 'prediction')

#calculate the accuracy
totalPred = predictions.count()
totalCorrect = predictions.filter(predictions.Survived == predictions.prediction).count()
print("Accuracy of test data: " + str(float(totalCorrect)/float(totalPred)))

predictionAndLabels = predictions.select('prediction', 'Survived').rdd
# Instantiate metrics object
metrics = BinaryClassificationMetrics(predictionAndLabels)

# Area under precision-recall curve
print("Area under PR = %s" % metrics.areaUnderPR)

# Area under ROC curve
print("Area under ROC = %s" % metrics.areaUnderROC)
#This will predict the survival rates of test data for submitting to kaggle
testVector = testIndexed.select(
    testIndexed.PassengerId,
    testIndexed.Pclass.cast("float"),
    testIndexed.Age.cast("float"),
    testIndexed.SibSp.cast("float"),
    testIndexed.Parch.cast("float"),
    testIndexed.Fare.cast("float"),
    testIndexed.sexIndex,
    testIndexed.embarkedIndex,
    )

testing = assembler.transform(testVector)
predictions = lrModel.transform(testing)

#Selects columns required for Kaggle submission
df = predictions.select(predictions.PassengerId, predictions.prediction.cast("int").alias("Survived"))
df.show(10)
df.toPandas().to_csv('submission.csv', index=False)