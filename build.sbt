import sbt.Keys.libraryDependencies

version in ThisBuild := "0.1"
scalacOptions in ThisBuild ++= Seq("-feature", "-deprecation")

val scalaCacheVersion = "0.28.0"
val gatlingVersion = "3.4.0"
val akkaVersion = "2.6.9"
val akkaHttpVersion = "10.2.0"

lazy val core = (project in file("core"))
  .settings(
    name := "throttling-service",
    scalaVersion := "2.13.3",
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.4.0",
      "com.github.cb372" %% "scalacache-core" % scalaCacheVersion,
      "com.github.cb372" %% "scalacache-caffeine" % scalaCacheVersion,
      "com.typesafe.akka" %% "akka-http"   % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.scalatest" %% "scalatest" % "3.2.0" % "test",
      "org.scalamock" %% "scalamock" % "5.0.0" % Test
    )
  )

lazy val gatling = (project in file("gatling"))
  .enablePlugins(GatlingPlugin)
  .settings(
    scalaVersion := "2.12.12",
    libraryDependencies ++= Seq(
      "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion % "test",
      "io.gatling"            % "gatling-test-framework"    % gatlingVersion % "test"
    )
  )//.dependsOn(core % "test -> run")

