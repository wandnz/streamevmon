
ThisBuild / resolvers ++= Seq(
  "Apache Development Snapshot Repository" at "https://repository.apache.org/content/repositories/snapshots/",
  Resolver.mavenLocal
)

ThisBuild / scalaVersion := "2.12.13"

import Dependencies._
import Licensing._
Licensing.applyLicenseOverrides

// These are settings that are shared between all submodules in this project.
lazy val sharedSettings = Seq(
  organization := "nz.net.wand",
  version := "0.3.0-RC1",
  scalacOptions ++= Seq("-deprecation", "-feature"),

  maintainer := "Daniel Oosterwijk <doosterw@waikato.ac.nz>",
  packageSummary := "Time series anomaly detection framework and pipeline",
  packageDescription :=
    """Streamevmon is a Flink-based framework for time-series anomaly detection.
      | It can ingest arbitrary data from a number of sources, and apply various
      | algorithms to the resulting data streams in real-time. It will then send
      | any detected events to a specified sink, such as InfluxDB.
      | .
      | Since it runs on Flink, streamevmon is capable of scaling horizontally
      | across many physical hosts. However, this would require manual
      | configuration.""".stripMargin,

  // Make run command from sbt console include Provided dependencies
  Compile / run := Defaults.runTask(Compile / run / fullClasspath,
    Compile / run / mainClass,
    Compile / run / runner
  ).evaluated,

  // Stay inside the sbt console when we press "ctrl-c" while a Flink programme executes with "run" or "runMain"
  Compile / run / fork := true,
  Global / cancelable := true,

  // Make tests in sbt shell more reliable
  fork := true,

  // Settings for `sbt doc`
  scalacOptions in(Compile, doc) ++= Seq(
    "-doc-title", name.value,
    "-doc-version", s"v${version.value}",
    "-diagrams"
  ),
  autoAPIMappings := true,
  apiMappings in doc ++= Dependencies.dependencyApiMappings((fullClasspath in Compile).value),
  apiURL := Dependencies.builtApiUrl,
)

// Core project does not depend on tunerDependencies, but does on everything else
lazy val root = (project in file("."))
  .settings(
    Seq(
      name := "streamevmon",
      libraryDependencies ++= providedDependencies ++ coreDependencies ++ testDependencies,
      mainClass := Some("nz.net.wand.streamevmon.runners.unified.YamlDagRunner"),
      annotationProcessingMapping := Map(
        "org.apache.logging.log4j.core.config.plugins.processor.PluginProcessor" -> Seq(
          "nz.net.wand.streamevmon.LoggingConfigurationFactory"
        )
      ),
    ) ++ sharedSettings ++ coreLicensing: _*
  )
  .enablePlugins(
    AnnotationProcessingPlugin,
    AutomateHeaderPlugin,
    AssemblyStreamevmonPlugin,
    DebianStreamevmonPlugin
  )

// Parameter tuner module depends on core project + SMAC dependencies
// We need to manually specify providedDependencies since % Provided modules
// are not inherited via dependsOn.
lazy val parameterTuner = (project in file("parameterTuner"))
  .dependsOn(root % "compile->compile;test->test")
  .settings(
    Seq(
      name := "parameterTuner",
      libraryDependencies ++= providedDependencies ++ tunerDependencies,
      unmanagedBase := baseDirectory.value / "lib",
      mainClass in assembly := Some("nz.net.wand.streamevmon.tuner.ParameterTuner"),
      fullClasspath in assembly := (fullClasspath in Compile).value,
      assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = true, includeDependency = true)
    ) ++ sharedSettings ++ AssemblyStreamevmonPlugin.assemblyStreamevmonSettings ++ parameterTunerLicensing: _*
  )
  .enablePlugins(AutomateHeaderPlugin)

// We make a configuration that lets us run `noDocker:test` to skip tests that
// require Docker.
lazy val NoDockerTests = config("noDocker") extend Test
// This is done by supplying arguments to testOnly, which is the only way to
// pass the `-l` argument to the underlying scalatest invocation. We do *
// instead of filtering the tests at all, since we just want to run everything
// that isn't annotated with TestContainersTest.
test in NoDockerTests := {
  (testOnly in Test).toTask(s" * -- -l nz.net.wand.streamevmon.test.TestContainersTest").value
}
