name := "reactive-design-patterns"

version := "1.0"

scalaVersion := "2.11.6"

description := "Sample code in reactive design pattern book"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.4.1",
    "com.typesafe.akka" %% "akka-cluster" % "2.4.1",
    "com.typesafe.play" %% "play-json" % "2.4.6"
  )
