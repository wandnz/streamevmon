ThisBuild / resolvers ++= Seq(
    "Apache Development Snapshot Repository" at "https://repository.apache.org/content/repositories/snapshots/",
    Resolver.mavenLocal
)

name := "Flink AMP Test"

version := "0.1-SNAPSHOT"

organization := "nz.ac.waikato"

ThisBuild / scalaVersion := "2.11.12"

val flinkVersion = "1.8.1"

val flinkDependencies = Seq(
  "org.apache.flink" %% "flink-scala" % flinkVersion,
  "org.apache.flink" %% "flink-streaming-scala" % flinkVersion)

val log4jDependencies = Seq(
  "org.apache.logging.log4j" %% "log4j-api-scala" % "11.0",
  "org.apache.logging.log4j" % "log4j-api" % "2.11.0",
  "org.apache.logging.log4j" % "log4j-core" % "2.11.0" % Runtime,
  "org.slf4j" % "slf4j-simple" % "1.7.9"
)

val chroniclerVersion = "0.5.1"

val influxDependencies = Seq(
  "com.github.fsanaulla" %% "chronicler-ahc-io" % chroniclerVersion,
  "com.github.fsanaulla" %% "chronicler-macros" % chroniclerVersion
)

lazy val root = (project in file(".")).
  settings(
    libraryDependencies ++=
      flinkDependencies ++
      influxDependencies ++
      log4jDependencies
  )

assembly / mainClass := Some("nz.ac.waikato.InfluxSubscription")

// make run command include the provided dependencies
Compile / run  := Defaults.runTask(Compile / fullClasspath,
                                   Compile / run / mainClass,
                                   Compile / run / runner
                                  ).evaluated

// stays inside the sbt console when we press "ctrl-c" while a Flink programme executes with "run" or "runMain"
Compile / run / fork := true
Global / cancelable := true

// exclude Scala library from assembly
assembly / assemblyOption  := (assembly / assemblyOption).value.copy(includeScala = false)
