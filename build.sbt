name := "SoftwareChallenge-TollBooth"

version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies ++=
  Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.5.16",
    "com.typesafe.akka" %% "akka-testkit" % "2.5.16" % Test,
    "org.scalatest" %% "scalatest" % "3.0.5" % Test)
