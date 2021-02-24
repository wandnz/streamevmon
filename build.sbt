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
  version := "0.1-SNAPSHOT",
  scalacOptions ++= Seq("-deprecation", "-feature"),

  // Settings for `sbt doc`
  scalacOptions in(Compile, doc) ++= Seq(
    "-doc-title", name.value,
    "-doc-version", version.value,
    "-diagrams"
  ),
  autoAPIMappings := true,
  // apiMappings code from https://stackoverflow.com/a/35673212
  apiMappings in doc ++= {
    def mappingsFor(organization: String, names: List[String], location: String, revision: String => String = identity): Seq[(File, URL)] =
      for {
        entry: Attributed[File] <- (fullClasspath in Compile).value
        module: ModuleID <- entry.get(moduleID.key)
        if module.organization == organization
        if names.exists(module.name.startsWith)
      } yield entry.data -> url(location.format(revision(module.revision)))

    val mappings: Seq[(File, URL)] =
      mappingsFor("org.scala-lang", List("scala-library"), "http://scala-lang.org/api/%s/") ++
        mappingsFor(
          "org.apache.flink",
          List("flink-"),
          "https://ci.apache.org/projects/flink/flink-docs-release-%s/api/java/",
          // The URL here only specifies up to minor releases, so we need to
          // drop the patch level of the version, eg 1.12.1 -> 1.12
          revision => revision.substring(0, revision.lastIndexOf('.'))
        ) ++
        mappingsFor(
          "org.apache.logging.log4j", List("log4j-api"),
          "https://logging.apache.org/log4j/2.x/log4j-api/apidocs/index.html"
        ) ++
        mappingsFor(
          "org.apache.logging.log4j", List("log4j-core"),
          "https://logging.apache.org/log4j/2.x/log4j-core/apidocs/index.html"
        )

    mappings.toMap
  },
  apiURL := Some(url("https://wanduow.github.io/streamevmon/")),

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

  // Stop JAR packaging from running tests first
  test in assembly := {},

  // META-INF gets special packaging behaviour depending on the subfolder
  // exclude META-INF from packaged JAR and use correct behaviour for duplicate library files
  assemblyMergeStrategy in assembly := {
    // Service definitions should all be concatenated
    case PathList("META-INF", "services", _@_*) => MergeStrategy.filterDistinctLines
    // Log4j2 plugin listings need special code to be merged properly.
    case PathList(ps@_*) if ps.last == "Log4j2Plugins.dat" => Log4j2MergeStrategy.strategy
    // The rest of META-INF gets tossed out.
    case PathList("META-INF", _@_*) => MergeStrategy.discard
    // We totally ignore the Java 11 module system... This produces runtime JVM
    // warnings, but it's not worth the effort to squash them since it doesn't
    // affect behaviour.
    case PathList("module-info.class") => MergeStrategy.discard
    // Everything else is kept as is.
    case other => (assemblyMergeStrategy in assembly).value(other)
  },
)

// Core project does not depend on tunerDependencies, but does on everything else
lazy val root = (project in file(".")).
  settings(
    Seq(
      name := "streamevmon",
      libraryDependencies ++= providedDependencies ++ coreDependencies ++ testDependencies,
      mainClass in assembly := Some("nz.net.wand.streamevmon.runners.unified.YamlDagRunner"),
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
      assembly / fullClasspath := (Compile / fullClasspath).value,
      assembly / assemblyOption := (assembly / assemblyOption).value.copy(includeScala = true, includeDependency = true)
    ) ++ sharedSettings ++ parameterTunerLicensing: _*
  )
  .enablePlugins(AutomateHeaderPlugin)

// Declare a few variants of the assembly command.
commands ++= AssemblyCommands.allCommands
commands ++= AssemblyCommands.WithScala.allCommands
AssemblyCommands.addAlias("assemble", AssemblyCommands.allCommands: _*)
AssemblyCommands.addAlias("assembleScala", AssemblyCommands.WithScala.allCommands: _*)
