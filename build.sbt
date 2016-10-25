name := "ssh-scp-akka-streams"

version := "1.0"

scalaVersion := "2.11.8"

val akkaV = "2.4.11"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaV,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaV,
  "com.typesafe.akka" %% "akka-http-core" % akkaV,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaV,
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
  "commons-codec" % "commons-codec" % "1.9",
  "ch.qos.logback" % "logback-classic" % "1.1.7",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
)