ThisBuild / resolvers ++= Seq(
  "Apache Development Snapshot Repository" at "https://repository.apache.org/content/repositories/snapshots/",
  Resolver.mavenLocal
)

ThisBuild / scalaVersion := "2.12.13"

import Dependencies._
import Licensing._
import com.typesafe.sbt.packager.linux.LinuxSymlink
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

  // exclude META-INF from packaged JAR and use correct behaviour for duplicate library files
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", "services", _@_*) => MergeStrategy.filterDistinctLines
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
    ) ++ sharedSettings ++ coreLicensing: _*
  )
  .enablePlugins(AutomateHeaderPlugin)

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

// DebianPlugin lets us make Debian packages. Sadly, there's a lot of manual
// overriding we have to do, and things like the Java Server Application
// archetype didn't end out being that useful.
enablePlugins(DebianPlugin)

version in Debian := s"${version.value}-1"
debianPackageDependencies := Seq("openjdk-11-jre-headless | java11-runtime-headless", "flink-scala2.12")
debianPackageProvides := Seq("streamevmon")

// deb packages aren't compressed by default by sbt-native-packager since
// jars are already compressed, but that makes lintian complain and we'd prefer
// to keep the number of lintian overrides at a minimum.
// The java build method doesn't support these flags and enforces no
// compression, along with a couple of other issues. We use the native build
// instead because of these issues.
debianNativeBuildOptions in Debian := Nil
debianChangelog := Some(file("src/debian/changelog"))

// packageBin doesn't normally require the md5sums file to be generated, but we want it.
packageBin in Debian := ((packageBin in Debian) dependsOn (debianMD5sumsFile in Debian)).value
linuxPackageMappings ++= Seq(
  // We've got to include some redundant mappings here since the native packager
  // creates parent folders with permission 0775, which is non-standard.
  packageTemplateMapping("/usr")().withPerms("0755"),
  packageTemplateMapping("/usr/lib")().withPerms("0755"),
  packageTemplateMapping("/usr/lib/streamevmon")().withPerms("0755"),
  packageTemplateMapping("/lib")().withPerms("0755"),
  packageTemplateMapping("/lib/systemd")().withPerms("0755"),
  packageTemplateMapping("/lib/systemd/system")().withPerms("0755"),
  packageTemplateMapping("/usr/share")().withPerms("0755"),
  packageTemplateMapping("/usr/share/lintian")().withPerms("0755"),
  packageTemplateMapping("/usr/share/lintian/overrides")().withPerms("0755"),
  packageTemplateMapping("/usr/share/doc")().withPerms("0755"),
  packageTemplateMapping("/usr/share/doc/streamevmon")().withPerms("0755"),
  packageTemplateMapping("/usr/share/streamevmon")().withPerms("0755"),
  // This is the main jar with all the program code
  packageMapping(
    file((Compile / packageBin / artifactPath).value.getParent + s"/${name.value}-nonProvidedDeps-${version.value}.jar") -> s"/usr/share/streamevmon/streamevmon.jar"
  ).withPerms("0644"),
  // These scripts are required for the systemd service
  packageMapping(
    file(s"${baseDirectory.value}/src/debian/streamevmon.service") -> "/lib/systemd/system/streamevmon.service"
  ).withPerms("0644"),
  packageMapping(
    file(s"${baseDirectory.value}/src/debian/taskmanager@.service") -> "/lib/systemd/system/taskmanager@.service"
  ).withPerms("0644"),
  // This contains any override directives for lintian, since there are a few
  // warnings that we know don't matter.
  packageMapping(
    file(s"${baseDirectory.value}/src/debian/lintian-overrides") -> "/usr/share/lintian/overrides/streamevmon"
  ).withPerms("0644"),
  // Despite the changelog file location being standard, sbt-native-packager
  // does not create a mapping from `debianChangelog` to its desired location.
  packageMapping(
    debianChangelog.value.get -> "/usr/share/doc/streamevmon/changelog.Debian.gz"
  ).gzipped.withPerms("0644"),
  packageMapping(
    file(s"${baseDirectory.value}/src/debian/copyright") -> "/usr/share/doc/streamevmon/copyright"
  ).withPerms("0644")
)
linuxPackageSymlinks ++= Seq(
  LinuxSymlink("/usr/share/flink/lib/streamevmon.jar", "/usr/share/streamevmon/streamevmon.jar")
)
