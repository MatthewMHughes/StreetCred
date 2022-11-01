# StreetCred

StreetCred is an app that takes a user search option for twitter and returns all tweets for that topic with a resulting credibility rating.

## Installation Instructions

It was an absolute pain yesterday trying to get this to build and run yesterday but this was roughly the process I followed without the 100 different google searches to fix all the problems I encountered.

1. clone this repository
2. install Scala and SBT: https://medium.com/analytics-vidhya/installing-scala-sbt-on-ubuntu-part-3-fd08fec70946
3. cd in StreetCredPlay
4. in the command line type: sbt sbtVersion - this will set the sbtVersion to the one used in the application so that all the dependencies work as expected
5. in the command line type: sbt -Dsbt.boot.directory=/tmp/boot1 -Dsbt.launcher.coursier=false - should hopefully run the sbt shell
6. in the command line type: run - this should hopefully start downloading all the dependencies and build the application hosting it at the ip address localhost:9000.
7. at localhost:9000 the web application should be running - if you go into top or recent searches you should see some strange behaviour of nine blank rectangles, this is because the mongodb local database isn't running. To fix this you will need to install the mongodb database tools: https://www.mongodb.com/docs/database-tools/installation/installation/ and type mongorestore to restore the database. https://www.mongodb.com/docs/database-tools/mongorestore/. You can host the local mongodb server by installing mongodb on your machine: https://www.mongodb.com/docs/manual/tutorial/install-mongodb-on-ubuntu/. You can then type: sudo systemctl start mongod - to launch the server and the application should connect to the database data.
