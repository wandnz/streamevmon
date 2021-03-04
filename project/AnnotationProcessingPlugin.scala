import sbt._
import sbt.Keys._

import scala.sys.process._

/** This plugin complements the compile step by running javac's annotation
  * processor over the produced class files, allowing Java annotations that
  * require special handling (like Log4j2 plugins) to be processed during a
  * Scala build.
  *
  * To use, the plugin must first be manually enabled on the project you want
  * to use it with. The `annotationProcessingMapping` settingKey should be set.
  * This setting is a map of the annotation processor's class name to a
  * collection of class names to be processed.
  */
object AnnotationProcessingPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin

  override def trigger = noTrigger

  object autoImport {
    val annotationProcess = taskKey[Unit]("Process annotations")
    val annotationProcessingMapping = settingKey[Map[String, Iterable[String]]](
      "Defines which annotation processors should process which user classes"
    )
    val annotationPrintRounds = settingKey[Boolean]("Enables -XprintRounds during annotation processing")
  }

  import autoImport._

  lazy val defaultAnnotationSettings = Seq(
    annotationPrintRounds := false,
    annotationProcess := AnnotationProcessor(
      annotationProcessingMapping = annotationProcessingMapping.value,
      classpath = ((products in Compile).value ++ (dependencyClasspath in Compile).value.files) mkString ":",
      destinationDirectory = (classDirectory in Compile).value,
      annotationPrintRounds = annotationPrintRounds.value,
      log = streams.value.log
    ),
    annotationProcess := annotationProcess.triggeredBy(compile in Compile).value
  )

  override lazy val projectSettings: Seq[Def.Setting[_]] = inConfig(Compile)(defaultAnnotationSettings)
}

/** This object runs the actual annotation processing. The code is adapted from
  * https://stackoverflow.com/a/34146473.
  */
object AnnotationProcessor {
  def apply(
    annotationProcessingMapping: Map[String, Iterable[String]],
    classpath: String,
    destinationDirectory: File,
    annotationPrintRounds: Boolean,
    log: Logger
  ): Unit = {
    log.info("Processing annotations ...")

    annotationProcessingMapping.foreach { case (processor, classesToProcess) =>
      val command = {
        val c = Seq(
          "javac", "-proc:only",
          "-processor", processor,
          "-d", destinationDirectory.getAbsolutePath,
          "-cp", classpath,
          classesToProcess.mkString(" ")
        )

        // We only add the -XprintRounds argument if it was requested by the user.
        if (annotationPrintRounds) {
          c.patch(2, List("-XprintRounds"), 0)
        }
        else {
          c
        }
      }

      log.debug(s"Running $command")
      val result = command.!
      if (result != 0) {
        log.error(s"Failed to process annotations using $processor for ${classesToProcess.mkString(" ")}")
        sys.error("Failed running command: " + command)
      }
    }

    log.info("Done processing annotations.")
  }
}
