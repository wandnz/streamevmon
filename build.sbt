ThisBuild / resolvers ++= Seq(
  "Apache Development Snapshot Repository" at "https://repository.apache.org/content/repositories/snapshots/",
  Resolver.mavenLocal
)

name := "Streamevmon"

version := "0.1-SNAPSHOT"

organization := "nz.net.wand"

ThisBuild / scalaVersion := "2.12.11"

val flinkVersion = "1.10.0"
val flinkDependencies = Seq(
  "org.apache.flink" %% "flink-scala" % flinkVersion % Provided,
  "org.apache.flink" %% "flink-streaming-scala" % flinkVersion % Provided,
  "org.apache.flink" %% "flink-table-api-scala" % flinkVersion % Provided
)

val chroniclerVersion = "0.6.4"
val influxDependencies = Seq(
  "com.github.fsanaulla" %% "chronicler-ahc-io" % chroniclerVersion,
  "com.github.fsanaulla" %% "chronicler-ahc-management" % chroniclerVersion,
  "com.github.fsanaulla" %% "chronicler-macros" % chroniclerVersion
)

val postgresDependencies = Seq(
  "org.postgresql" % "postgresql" % "42.2.12",
  "org.squeryl" %% "squeryl" % "0.9.14"
)

val scalaCacheVersion = "0.28.0"
val cacheDependencies = Seq(
  "com.github.cb372" %% "scalacache-core" % scalaCacheVersion,
  "com.github.cb372" %% "scalacache-caffeine" % scalaCacheVersion,
  "com.github.cb372" %% "scalacache-memcached" % scalaCacheVersion
)

val logDependencies = Seq(
  "org.slf4j" % "slf4j-simple" % "1.7.30" % Provided
)

val testcontainersScalaVersion = "0.36.1"
val testDependencies = Seq(
  "org.apache.flink" %% "flink-test-utils" % flinkVersion % Test,
  "org.apache.flink" %% "flink-runtime" % flinkVersion % Test classifier "tests",
  "org.apache.flink" %% "flink-streaming-java" % flinkVersion % Test classifier "tests",
  "org.scalatest" %% "scalatest" % "3.1.1" % Test,
  "com.dimafeng" %% "testcontainers-scala" % "0.36.1" % Test,
  "com.dimafeng" %% "testcontainers-scala-scalatest" % testcontainersScalaVersion % Test,
  "com.dimafeng" %% "testcontainers-scala-postgresql" % testcontainersScalaVersion % Test,
  "com.dimafeng" %% "testcontainers-scala-influxdb" % testcontainersScalaVersion % Test
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

mainClass in assembly := Some("nz.net.wand.streamevmon.runners.UnifiedRunner")
assemblyExcludedJars in assembly := {
  (fullClasspath in assembly).value.filter(_.data.getName.contains("scalatest"))
}

scalacOptions ++= Seq("-deprecation", "-feature")

// Make tests in sbt shell more reliable
fork := true

// Stop assembly from running tests first
test in assembly := {}

// exclude Scala library from assembly
//assembly / assemblyOption := (assembly / assemblyOption).value.copy(includeScala = false, includeDependency = false)
assembly / assemblyOption := (assembly / assemblyOption).value.copy(includeScala = false, includeDependency = true)
assemblyPackageDependency / assemblyOption := (assemblyPackageDependency / assemblyOption).value.copy(includeScala = false)

// exclude META-INF and use correct behaviour for duplicate library files
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", _@_*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}
