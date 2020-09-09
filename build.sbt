ThisBuild / resolvers ++= Seq(
  "Apache Development Snapshot Repository" at "https://repository.apache.org/content/repositories/snapshots/",
  Resolver.mavenLocal
)

ThisBuild / scalaVersion := "2.12.12"

import Dependencies._

// These are settings that are shared between all submodules in this project.
lazy val sharedSettings = Seq(
  // Set up simple metadata and compiler options
  organization := "nz.net.wand",
  version := "0.1-SNAPSHOT",
  scalacOptions ++= Seq("-deprecation", "-feature"),

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

  // exclude META-INF from packaged JAR and use correct behaviour for duplicate library files
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", _@_*) => MergeStrategy.discard
    case PathList("module-info.class") => MergeStrategy.discard
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
    ) ++ sharedSettings
  )

// Parameter tuner module depends on core project + SMAC dependencies
// We need to manually specify providedDependencies since % Provided modules
// are not inherited via dependsOn.
lazy val parameterTuner = (project in file("parameterTuner"))
  .dependsOn(root)
  .settings(
    Seq(
      name := "parameterTuner",
      libraryDependencies ++= providedDependencies ++ tunerDependencies,
      unmanagedBase := baseDirectory.value / "lib",
      mainClass in assembly := Some("nz.net.wand.streamevmon.tuner.ParameterTuner"),
      assembly / fullClasspath := (Compile / fullClasspath).value,
      assembly / assemblyOption := (assembly / assemblyOption).value.copy(includeScala = true, includeDependency = true)
    ) ++ sharedSettings
  )

// Declare a few variants of the assembly command.
commands ++= AssemblyCommands.allCommands
commands ++= AssemblyCommands.WithScala.allCommands
AssemblyCommands.addAlias("assemble", AssemblyCommands.allCommands: _*)
