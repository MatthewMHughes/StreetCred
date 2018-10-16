# A script to translate the json tweets from the training data and assigning them a boolean credibility score

from pyspark.context import SparkContext
from pyspark.sql import *
from pyspark.ml.feature import *

sc = SparkContext("local", "tweetConverter")
sqlContext = SQLContext(sc)

tweets = sqlContext.read.format("csv").option("header", "true").load("csv")
labels = sqlContext.read.format("csv").option("header", "true").load("credibility.labels.csv")

# joins tweets with their credibility rating by joining on id. This is due to huge number of deleted tweets in data set.
tweets = tweets.join(labels, tweets.id == labels.cred_id)
print("Number of tweets in the data set is " + str(tweets.count()))

# filter all non-English tweets
tweets = tweets.filter(tweets.lang == "en")
print("Number of tweets in English is " + str(tweets.count()))

# Convert verified/unverified into a boolean using index
credIndexer = StringIndexer(inputCol="cred", outputCol="credIndex")
credLabeller = credIndexer.fit(tweets)
tweets = credLabeller.transform(tweets)

tweets.toPandas().to_csv('training.csv', index=True)
