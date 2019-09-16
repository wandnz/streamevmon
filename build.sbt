ThisBuild / resolvers ++= Seq(
  "Apache Development Snapshot Repository" at "https://repository.apache.org/content/repositories/snapshots/",
  Resolver.mavenLocal
)

name := "Streamevmon"

version := "0.1-SNAPSHOT"

organization := "nz.net.wand"

ThisBuild / scalaVersion := "2.12.9"

val flinkVersion = "1.9.0"
val flinkDependencies = Seq(
  "org.apache.flink" %% "flink-scala" % flinkVersion,
  "org.apache.flink" %% "flink-streaming-scala" % flinkVersion,
  "org.apache.flink" %% "flink-table-api-scala" % flinkVersion
)

val chroniclerVersion = "0.5.5"
val influxDependencies = Seq(
  "com.github.fsanaulla" %% "chronicler-ahc-io" % chroniclerVersion,
  "com.github.fsanaulla" %% "chronicler-ahc-management" % chroniclerVersion,
  "com.github.fsanaulla" %% "chronicler-macros" % chroniclerVersion
)

val postgresDependencies = Seq(
  "org.postgresql" % "postgresql" % "9.4.1212",
  "org.squeryl" %% "squeryl" % "0.9.14"
)

val scalaCacheVersion = "0.28.0"
val cacheDependencies = Seq(
  "com.github.cb372" %% "scalacache-core" % scalaCacheVersion,
  "com.github.cb372" %% "scalacache-caffeine" % scalaCacheVersion,
  "com.github.cb372" %% "scalacache-memcached" % scalaCacheVersion
)

val logDependencies = Seq(
  "org.slf4j" % "slf4j-simple" % "1.7.28"
)

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "com.dimafeng" %% "testcontainers-scala" % "0.30.0" % "test",
  "org.testcontainers" % "postgresql" % "1.12.0" % "test",
  "org.testcontainers" % "influxdb" % "1.12.0" % "test"
)

lazy val root = (project in file(".")).
  settings(
    libraryDependencies ++=
      flinkDependencies ++
      influxDependencies ++
      postgresDependencies ++
      cacheDependencies ++
      logDependencies ++
      testDependencies
  )

// make run command include the provided dependencies
Compile / run := Defaults.runTask(Compile / run / fullClasspath,
                                   Compile / run / mainClass,
                                   Compile / run / runner
                                  ).evaluated

// stays inside the sbt console when we press "ctrl-c" while a Flink programme executes with "run" or "runMain"
Compile / run / fork := true
Global / cancelable := true

mainClass in assembly := Some("nz.net.wand.streamevmon.runners.ChangepointRunner")

// Make tests in sbt shell more reliable
fork := true

// Stop assembly from running tests first
test in assembly := {}

// exclude Scala library from assembly
assembly / assemblyOption := (assembly / assemblyOption).value.copy(includeScala = false)

// exclude META-INF and use correct behaviour for duplicate library files
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", _@_*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}
