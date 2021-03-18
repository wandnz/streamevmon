import sbt._
import sbt.Keys._
import sbtassembly.Assembly.isScalaLibraryFile
import sbtassembly.AssemblyPlugin
import sbtassembly.AssemblyPlugin.autoImport._

/** This plugin specifies our project settings for sbt assembly, which lets us
  * produce a fat JAR file containing any required dependencies for distribution.
  *
  * It also declares a number of custom configurations, allowing us to include
  * various configurations of dependencies in our assembly file. The variants
  * are as follows, and various combinations of the settings are defined.
  *
  * - Include/exclude project code
  * - Include/exclude scala distribution
  * - Include/exclude Provided dependencies
  * - Include/exclude regular dependencies
  *
  * Finally, we declare some meta-tasks to run various groups of these settings.
  * `assemblyAll` will run all ten (10) configurations.
  *
  * This plugin is triggered automatically, and does not need to be loaded.
  */
object AssemblyStreamevmonPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin && AssemblyPlugin

  override def trigger = noTrigger

  /** Our global settings. We stop tests from being run before assembly, and
    * define an assemblyMergeStrategy so the resulting JAR is functional.
    */
  lazy val assemblyStreamevmonSettings = Seq(
    // Stop JAR packaging from running tests first
    test in assembly := {},
    // This lets us define correct behaviour for META-INF subfolders and other
    // misc files that don't need to just be included.
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
    }
  )

  object autoImport {
    private lazy val defaults = Compile
    lazy val ProjectOnly = config("projectOnly") extend defaults
    lazy val ProjectAndNonProvidedDeps = config("projectAndNonProvidedDeps") extend defaults
    lazy val ProjectAndAllDeps = config("projectAndDeps") extend defaults
    lazy val NonProvidedDeps = config("onlyNonProvidedDeps") extend defaults
    lazy val OnlyDeps = config("onlyDeps") extend defaults
    lazy val ProjectAndScala = config("projectAndScala") extend defaults
    lazy val ProjectAndScalaAndNonProvidedDeps = config("projectAndScalaAndNonProvidedDeps") extend defaults
    lazy val ProjectAndScalaAndAllDeps = config("projectAndScalaAndDeps") extend defaults
    lazy val ScalaAndNonProvidedDeps = config("scalaAndNonProvidedDeps") extend defaults
    lazy val ScalaAndAllDeps = config("scalaAndDeps") extend defaults

    lazy val noScalaConfigs = Seq(ProjectOnly, ProjectAndNonProvidedDeps, ProjectAndAllDeps, NonProvidedDeps, OnlyDeps)
    lazy val withScalaConfigs = Seq(ProjectAndScala, ProjectAndScalaAndNonProvidedDeps, ProjectAndScalaAndAllDeps, ScalaAndNonProvidedDeps, ScalaAndAllDeps)
    lazy val allConfigs: Seq[Configuration] = noScalaConfigs ++ withScalaConfigs

    lazy val assemblyAllNoScala = taskKey[Unit]("Run all assembly configurations (5) that don't package Scala")
    lazy val assemblyAllScala = taskKey[Unit]("Run all assembly configurations (5) that package Scala")
    lazy val assemblyAll = taskKey[Unit]("Run all assembly configurations (10)")
  }

  import autoImport._

  private object DependencyLevel extends Enumeration {
    val None: Value = Value
    val NonProvided: Value = Value
    val All: Value = Value
  }

  private case class AssemblySettings(
    includeProject     : Boolean,
    includeScala       : Boolean,
    includeDependencies: DependencyLevel.Value
  )

  private object AssemblySettings {
    def apply(config: Configuration): AssemblySettings = {
      config match {
        case ProjectOnly => AssemblySettings(includeProject = true, includeScala = false, DependencyLevel.None)
        case ProjectAndNonProvidedDeps => AssemblySettings(includeProject = true, includeScala = false, DependencyLevel.NonProvided)
        case ProjectAndAllDeps => AssemblySettings(includeProject = true, includeScala = false, DependencyLevel.All)
        case NonProvidedDeps => AssemblySettings(includeProject = false, includeScala = false, DependencyLevel.NonProvided)
        case OnlyDeps => AssemblySettings(includeProject = false, includeScala = false, DependencyLevel.All)
        case ProjectAndScala => AssemblySettings(includeProject = true, includeScala = true, DependencyLevel.None)
        case ProjectAndScalaAndNonProvidedDeps => AssemblySettings(includeProject = true, includeScala = true, DependencyLevel.NonProvided)
        case ProjectAndScalaAndAllDeps => AssemblySettings(includeProject = true, includeScala = true, DependencyLevel.All)
        case ScalaAndNonProvidedDeps => AssemblySettings(includeProject = false, includeScala = true, DependencyLevel.NonProvided)
        case ScalaAndAllDeps => AssemblySettings(includeProject = false, includeScala = true, DependencyLevel.All)
        case _ => AssemblySettings(includeProject = true, includeScala = false, DependencyLevel.NonProvided)
      }
    }
  }

  /** Defines the required settings within the provided Configuration */
  def constructConfigEntry(config: Configuration): Seq[Def.Setting[_]] = {
    inConfig(config)(AssemblyPlugin.assemblySettings ++ assemblyStreamevmonSettings ++ constructSettings(config))
  }

  /** Constructs the part of the settings that changes depending on config */
  def constructSettings(
    config: Configuration
  ): Seq[Def.Setting[_]] = {
    val settings = AssemblySettings(config)

    /** This definition is borrowed from the Assembly plugin's `Assembly.scala`
      */
    def scalaLibraries(scalaVersion: String) = {
      val scalaPre213Libraries = Vector(
        "scala-actors",
        "scala-compiler",
        "scala-continuations",
        "scala-library",
        "scala-parser-combinators",
        "scala-reflect",
        "scala-swing",
        "scala-xml")
      val scala213AndLaterLibraries = Vector(
        "scala-actors",
        "scala-compiler",
        "scala-continuations",
        "scala-library",
        "scala-reflect")

      val scalaVersionParts = VersionNumber(scalaVersion)
      val isScala213AndLater = scalaVersionParts.numbers.length >= 2 && scalaVersionParts._1.get >= 2 && scalaVersionParts._2.get >= 13
      if (isScala213AndLater) {
        scala213AndLaterLibraries
      }
      else {
        scalaPre213Libraries
      }
    }

    /** Gets rid of everything that isn't scala-library and the project
      */
    def removeAllDependencies(scalaVersion: String, buildDir: File, cp: Classpath): Classpath = {
      cp.filter { file =>
        isScalaLibraryFile(scalaLibraries(scalaVersion), file.data.asFile) ||
          file.data.asFile.getAbsolutePath.startsWith(buildDir.getAbsolutePath)
      }
    }

    /** Optionally removes scala-library from the provided classpath
      */
    def maybeRemoveScala(scalaVersion: String, cp: Classpath): Classpath = {
      if (!settings.includeScala) {
        cp.filterNot { file =>
          isScalaLibraryFile(scalaLibraries(scalaVersion), file.data.asFile)
        }
      }
      else {
        cp
      }
    }

    /** Optionally removes the project code from the provided classpath
      */
    def maybeRemoveProject(buildDir: File, cp: Classpath): Classpath = {
      if (!settings.includeProject) {
        cp.filterNot { file =>
          file.data.asFile.getAbsolutePath.startsWith(buildDir.getAbsolutePath)
        }
      }
      else {
        cp
      }
    }

    Seq(
      // Add the configuration to the resulting jar name.
      assemblyJarName := s"${name.value}-${config.name}-${version.value}.jar",
      // Defines the classpath that gets placed into the assembly. We do this
      // manually since it gives us more flexibility than assembly's options.
      fullClasspath := {
        // We might filter out the project code...
        maybeRemoveProject(
          (classDirectory in Compile).value,
          // We might filter out scala...
          maybeRemoveScala(
            scalaVersion.value,
            // And we might filter out a subset (or all) dependencies.
            settings.includeDependencies match {
              case DependencyLevel.None => removeAllDependencies(
                scalaVersion.value,
                (classDirectory in Compile).value,
                (fullClasspath in Compile).value
              )
              case AssemblyStreamevmonPlugin.DependencyLevel.NonProvided => (fullClasspath in Runtime).value
              case AssemblyStreamevmonPlugin.DependencyLevel.All => (fullClasspath in Compile).value
            }
          )
        )
      }
    )
  }

  /** We also define a few meta-tasks that just run some of the other tasks
    * we defined, and re-define the default assembly to run a particular config.
    */
  lazy val assemblyCommandDefinitions = Seq(
    assemblyAllNoScala := Unit,
    assemblyAllNoScala := assemblyAllNoScala.dependsOn(
      noScalaConfigs.map(conf => assembly in conf): _*
    ).value,
    assemblyAllScala := Unit,
    assemblyAllScala := assemblyAllScala.dependsOn(
      withScalaConfigs.map(conf => assembly in conf): _*
    ).value,
    assemblyAll := Unit,
    assemblyAll := assemblyAll.dependsOn(assemblyAllNoScala, assemblyAllScala, assemblyPackageScala).value,
    assembly := {
      (assembly in ProjectAndNonProvidedDeps).value
    }
  )

  /** Finally, we apply all the settings we just made. They also get applied to
    * the default scope of assembly, although we did just redefine it so that
    * shouldn't matter.
    */
  override lazy val projectSettings: Seq[Def.Setting[_]] = assemblyStreamevmonSettings ++
    assemblyCommandDefinitions ++
    allConfigs.flatMap(constructConfigEntry)
}
