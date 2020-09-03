name := "throttling-service"

version := "0.1"

scalaVersion := "2.13.3"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.4.0",
  "com.github.cb372" %% "scalacache-core" % "0.28.0",
  "com.github.cb372" %% "scalacache-caffeine" % "0.28.0",
  "com.typesafe.akka" %% "akka-http"   % "10.1.12",
  "com.typesafe.akka" %% "akka-stream" % "2.5.26",
  "org.scalatest" %% "scalatest" % "3.2.0" % "test",
  "org.scalamock" %% "scalamock" % "5.0.0" % Test
)