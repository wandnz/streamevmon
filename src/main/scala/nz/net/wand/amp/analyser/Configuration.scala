package nz.net.wand.amp.analyser

import java.nio.file.{Files, Paths}

import org.apache.flink.api.java.utils.ParameterTool

/** Common configuration setup for Flink jobs. The result of get should be
  * set as the global job parameters.
  */
object Configuration {
  /** Constructs a ParameterTool from a number of sources, each of which
    * override the previous.
    *
    * First, the default configuration is loaded.
    *
    * Second, a file called `amp-analyser.properties` in "./conf" (relative to
    * the working directory) is searched for and loaded if present. This file
    * can be used to provide environment-specific configuration options when it
    * is inconvenient to adjust the system properties or program arguments.
    *
    * Next, the system properties are loaded.
    *
    * Finally, the program arguments are loaded.
    *
    * @param args The program arguments, passed from main.
    *
    * @return A ParameterTool object containing the union of all the arguments
    *         collected.
    */
  def get(args: Array[String]): ParameterTool = {
    val defaults = ParameterTool.fromPropertiesFile(
      getClass.getClassLoader.getResourceAsStream("default.properties")
    )

    val withCustomFile = {
      val path = "conf/amp-analyser.properties"
      if (Files.exists(Paths.get(path))) {
        defaults.mergeWith(
          ParameterTool.fromPropertiesFile(path)
        )
      }
      else {
        defaults
      }
    }

    withCustomFile
      .mergeWith(ParameterTool.fromSystemProperties())
      .mergeWith(ParameterTool.fromArgs(args))
  }
}
