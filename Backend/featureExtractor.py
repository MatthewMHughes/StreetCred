from pyspark.context import SparkContext
from pyspark.sql import *
from pyspark.sql.functions import *
from pyspark.sql.column import *
from pyspark.ml.feature import *
from pyspark.ml.feature import VectorAssembler

sc = SparkContext("local", "featureExtractor")
sqlContext = SQLContext(sc)

tweets = sqlContext.read.format("csv").option("header", "true").load("csv")
labels = sqlContext.read.format("csv").option("header", "true").load("credibility.labels.csv")

# joins tweets with their credibility rating by joining on id. This is due to huge number of deleted tweets in data set.
tweets = tweets.join(labels, tweets.id == labels.cred_id)

# filter all non-English tweets
tweets = tweets.filter(tweets.lang == "en")

# Convert verified/unverified into a boolean using index
credIndexer = StringIndexer(inputCol="cred", outputCol="credIndex")
credLabeller = credIndexer.fit(tweets)
tweets = credLabeller.transform(tweets)

# Adds column with number of characters in a tweet
tweets = tweets.withColumn('characterCount', length(tweets.text))

# Adds column with number of words in a tweet
tweets = tweets.withColumn('wordCount', size(split(tweets.text, ' ')))

# Binary column which is 1.0 if the tweet contains an url, 0.0 otherwise
tweets = tweets.withColumn('containsUrl', tweets.urls.isNotNull().cast('float'))

# Adds column with number of hashtags in a tweet - converts -1 given to null hashtags to 0
tweets = tweets.withColumn('hashtagsCount', size(split(tweets.hashtags, ' ')))
tweets = tweets.replace(-1, 0)

# Binary column which is 1.0 if the tweet contains a geolocation, 0.0 otherwise
tweets = tweets.withColumn('containsGeoLocation', tweets.coordinates.isNotNull().cast('float'))

# Binary column which is 1.0 if the user is verified, 0.0 otherwise
# tweets = tweets.withColumn('userVerified', )
tweets = tweets.fillna({'retweet_count': "0"})
tweets = tweets.fillna({'user_friends_count': "0"})
tweets = tweets.fillna({'user_followers_count': "0"})

features = tweets.select(tweets.retweet_count.cast("float"),
                         tweets.user_friends_count.cast("float").alias('friends'),
                         tweets.user_followers_count.cast("float").alias('followers'),
                         tweets.characterCount,
                         tweets.wordCount,
                         tweets.containsUrl,
                         tweets.hashtagsCount.cast("float"),
                         tweets.containsGeoLocation,
                         tweets.credIndex)

assembler = VectorAssembler(
    inputCols=["retweet_count", "characterCount", "wordCount", "containsUrl", "hashtagsCount", "containsGeoLocation"],
    outputCol="features")

training = assembler.transform(features)

training.show()

(train_cv, test_cv) = training.randomSplit([0.7, 0.3])

#This will create a logistic regression model of our data
from pyspark.ml.classification import LogisticRegression
from pyspark.mllib.evaluation import BinaryClassificationMetrics
from pyspark.mllib.regression import LabeledPoint
lr = LogisticRegression(featuresCol = 'features', labelCol = 'credIndex', maxIter=10)
lrModel = lr.fit(train_cv)

#This will predict on our test using lr model
predictions = lrModel.transform(test_cv)
predictions.select('credIndex', 'prediction')

#calculate the accuracy
totalPred = predictions.count()
totalCorrect = predictions.filter(predictions.credIndex == predictions.prediction).count()
print("Accuracy of test data: " + str(float(totalCorrect)/float(totalPred)))

predictionAndLabels = predictions.select('prediction', 'credIndex').rdd
# Instantiate metrics object
metrics = BinaryClassificationMetrics(predictionAndLabels)

# Area under precision-recall curve
print("Area under PR = %s" % metrics.areaUnderPR)

# Area under ROC curve
print("Area under ROC = %s" % metrics.areaUnderROC)
