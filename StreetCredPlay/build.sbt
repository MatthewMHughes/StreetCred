name := "StreetCredPlay"
 
version := "1.0" 
      
lazy val `streetcredplay` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

resolvers += Resolver.sonatypeRepo("releases")
      
scalaVersion := "2.11.12"

libraryDependencies += "org.apache.spark" %% "spark-core" % "2.3.2"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.3.2"
libraryDependencies += "org.apache.spark" %% "spark-mllib" % "2.3.2"
libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "2.4.2"
libraryDependencies += "org.mongodb.spark" %% "mongo-spark-connector" % "2.3.1"
libraryDependencies += "com.danielasfregola" %% "twitter4s" % "5.5"
libraryDependencies += "org.twitter4j" % "twitter4j-stream" % "4.0.7"
libraryDependencies += "org.apache.hadoop" % "hadoop-mapreduce-client-core" % "2.7.2"
libraryDependencies += "org.apache.hadoop" % "hadoop-common" % "2.7.2"
libraryDependencies += "commons-io" % "commons-io" % "2.4"
libraryDependencies ++= Seq( jdbc , ehcache , ws , specs2 % Test , guice )

dependencyOverrides ++= Set("com.fasterxml.jackson.core" % "jackson-databind" % "2.6.7.1")

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )