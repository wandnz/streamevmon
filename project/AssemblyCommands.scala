import sbt._
import sbt.internal.DslEntry

import scala.collection.mutable

/** This object declares commands for various configurations of the sbt assembly
  * command. It allows us to specify at the shell what parts of the code and
  * dependencies we want to include in the produced JAR.
  *
  * Commands are called with the following format, depending on what should be
  * included. Not all combinations are available, and the loaded commands depend
  * on what you've added to the `commands` key.
  *
  * `sbt> assembly-scala-project-allDeps`
  *
  * `scala` and `project` can be removed, to make something like `assembly-allDeps`.
  * `allDeps` can be replaced with `nonProvidedDeps`, or removed.
  *
  * This object also offers a way to add an alias that executes a number of
  * provided commands in order.
  */
object AssemblyCommands {

  private object DependencyLevel extends Enumeration {
    val None: Value = Value
    val NonProvided: Value = Value
    val All: Value = Value
  }

  private def generateAssemblyCommand(
    includeProject: Boolean = true,
    includeDependencies: DependencyLevel.Value = DependencyLevel.NonProvided,
    includeScala: Boolean = false
  ): Command = {

    val nameBuf = mutable.Buffer[String]()
    if (includeScala) {
      nameBuf.append("scala")
    }
    if (includeProject) {
      nameBuf.append("project")
    }
    includeDependencies match {
      case DependencyLevel.None =>
      case DependencyLevel.NonProvided => nameBuf.append("nonProvidedDeps")
      case DependencyLevel.All => nameBuf.append("allDeps")
    }

    val name = nameBuf.mkString("-", "-", "")

    // Constructing commands has to be done via these strings, due to how :: is
    // defined in StateOps. If `set` is removed, the strings should be valid
    // Scala(sbt) code.
    Command.command(s"assembly$name") { state =>
      val optionallyIncludeProvidedString = if (includeDependencies == DependencyLevel.All) {
        """set assembly / fullClasspath := (Compile / fullClasspath).value"""
      }
      else {
        ""
      }
      optionallyIncludeProvidedString ::
        s"""set assembly / assemblyOption := (assembly / assemblyOption).value.copy(includeScala = $includeScala, includeDependency = ${includeDependencies != DependencyLevel.None})""" ::
        """set assembly / assemblyJarName := s"${name.value}""" + name + """-${version.value}.jar"""" ::
        "assembly" :: state
    }
  }

  // These are the public fields, defining all the commands we want.
  lazy val ProjectOnly = generateAssemblyCommand(includeProject = true, DependencyLevel.None)
  lazy val ProjectAndNonProvidedDeps = generateAssemblyCommand(includeProject = true, DependencyLevel.NonProvided)
  lazy val ProjectAndAllDeps = generateAssemblyCommand(includeProject = true, DependencyLevel.All)
  lazy val NonProvidedDepsOnly = generateAssemblyCommand(includeProject = false, DependencyLevel.NonProvided)
  lazy val AllDepsOnly = generateAssemblyCommand(includeProject = false, DependencyLevel.All)
  lazy val allCommands = Seq(ProjectOnly, ProjectAndNonProvidedDeps, ProjectAndAllDeps, NonProvidedDepsOnly, AllDepsOnly)

  object WithScala {
    lazy val ProjectOnly = generateAssemblyCommand(includeProject = true, DependencyLevel.None, includeScala = true)
    lazy val ProjectAndNonProvidedDeps = generateAssemblyCommand(includeProject = true, DependencyLevel.NonProvided, includeScala = true)
    lazy val ProjectAndAllDeps = generateAssemblyCommand(includeProject = true, DependencyLevel.All, includeScala = true)
    lazy val NonProvidedDepsOnly = generateAssemblyCommand(includeProject = false, DependencyLevel.NonProvided, includeScala = true)
    lazy val AllDepsOnly = generateAssemblyCommand(includeProject = false, DependencyLevel.All, includeScala = true)

    lazy val allCommands = Seq(ProjectOnly, ProjectAndNonProvidedDeps, ProjectAndAllDeps, NonProvidedDepsOnly, AllDepsOnly)
  }

  def addAlias(name: String, commands: Command*): DslEntry = {
    addCommandAlias(name, commands.map(_.nameOption.get).mkString(";", ";", ""))
  }
}
