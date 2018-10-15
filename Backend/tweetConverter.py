from pyspark.context import SparkContext
from pyspark.sql import *
from pyspark.sql.functions import explode

sc = SparkContext("local", "Titanic")
sqlContext = SQLContext(sc)

tweets = sqlContext.read.format("csv").option("header", "true").load("csv")

tweets.printSchema()