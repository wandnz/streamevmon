package nz.net.wand.streamevmon

import nz.net.wand.streamevmon.runners.unified.schema.FlowSchema

import java.io.{File, FileInputStream, InputStream}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.flink.api.java.utils.ParameterTool
import org.snakeyaml.engine.v2.api.{Load, LoadSettings}

import scala.collection.JavaConverters._
import scala.util.Try

/** Common configuration setup for Flink jobs. The result of get should be
  * set as the global job parameters.
  */
object Configuration {

  /** Constructs a ParameterTool from a number of sources, each of which
    * override the previous.
    *
    * First, the default configuration is loaded.
    *
    * Second, a file called `streamevmon.properties` in "./conf" (relative to
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
  def get(args: Array[String]): ParameterTool = getFromYaml(args)

  def get(): ParameterTool = get(Array())

  /** Flattens a `Map[_, Map[_, _]]` to a `Map[String, String]`, such
    * that nested maps are flattened into several dot-separated keys. All keys
    * and values have toString called on them in the process, which should
    * remain meaningful for all the primitive types being encoded.
    *
    * The following two maps show an example input and output of this function.
    *
    * {{{
    * Map(
    *   "topLevel" -> Map(
    *     "secondLevel" -> true,
    *     "thirdLevel" -> false
    *   ),
    * "secondLevel" -> false
    * )
    *
    * Map(
    *   "topLevel.secondLevel" -> true
    *   "topLevel.thirdLevel" -> false
    *   "secondLevel" -> false
    * )
    * }}}
    *
    * Java classes are converted into Scala classes, then passed back into the
    * function. Lists are handled as though they are just several entries in the
    * original map. This isn't a perfect representation of YAML structure, but
    * it works fine for our purposes.
    */
  def flattenMap(map: Map[String, _]): Map[String, String] = {
    map.flatMap {
      case (k, v) => v match {
        // If we're passed a Java map, just convert it to Scala and try again.
        // We really want to use the Scala map() function instead of messing
        // around with iterators and mutable holder buffers.
        case jMap: java.util.Map[_, _] => flattenMap(Map(k -> jMap.asScala.toMap))
        // If we're passed a Java list, it will most likely contain a series of
        // either additional maps, or leaves. Maps should be passed back in,
        // while leaves should be converted to String and returned.
        case jList: java.util.List[_] =>
          jList.asScala.flatMap {
            case item: java.util.Map[_, _] => flattenMap(Map(k -> item.asScala.toMap))
            case item: Map[_, _] => flattenMap(Map(k -> item))
            case leaf => Map(k -> leaf.toString)
          }
        // If we're given a map, just pass it back into the function. When we
        // hit a leaf, it will return a Map(deeperKey -> leaf). We want to
        // prepend the key at our current level to the deeper key, and pass it
        // back up the chain.
        case map: Map[_, _] =>
          val ensureStringKeys = map.map {
            case (a, b) => (a.toString, b)
          }
          flattenMap(ensureStringKeys).map {
            case (key, value) => (
              key match {
                case "" => key
                case _ => s"$k.$key"
              },
              value
            )
          }
        // Leaves just get converted to Strings and returned.
        case leaf => Map(k -> leaf.toString)
      }
    }
  }

  private def getFromYaml(args: Array[String]): ParameterTool = {
    val loader = new Load(LoadSettings.builder().build())

    def parameterToolFromYamlStream(yamlStream: InputStream): ParameterTool = {
      // Yes, we convert to Scala collections for flattenMap, and then back to
      // Java again. The map() function is just too nice to pass up.
      ParameterTool.fromMap(
        flattenMap(
          Option(loader.loadFromInputStream(yamlStream))
            .getOrElse(new java.util.HashMap[String, Any]())
            .asInstanceOf[java.util.Map[String, Any]]
            .asScala.toMap
        )
          .asJava
      )
    }

    val defaultSettingsFiles = Seq(
      "generalSettings.yaml",
      "connectorSettings.yaml",
      "detectorSettings.yaml"
    ).map(getClass.getClassLoader.getResourceAsStream)

    val customSettingsFiles = new File("conf").listFiles(
      (_: File, name: String) =>
        (name.endsWith(".yaml") || name.endsWith(".yml")) && (name != "flows.yaml")
    ).sorted.map(new FileInputStream(_))

    val pTools = (defaultSettingsFiles ++ customSettingsFiles).map { f =>
      parameterToolFromYamlStream(f)
    }

    (
      pTools :+
        ParameterTool.fromSystemProperties() :+
        ParameterTool.fromArgs(args)
      ).foldLeft(
      ParameterTool.fromArgs(Array())
    ) {
      (p1, p2) => p1.mergeWith(p2)
    }
  }

  def getFlowsDag: FlowSchema = {
    val loader = new Load(LoadSettings.builder.build)
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

    val loadedYaml = Try(loader.loadFromInputStream(
      new FileInputStream(new File("conf/flows.yaml"))
    )).toOption match {
      case None | Some(null) =>
        val defaults = loader.loadFromInputStream(
          getClass.getClassLoader.getResourceAsStream("flows.yaml")
        )
        defaults
      case Some(value) =>
        value
    }

    val result = mapper.convertValue(loadedYaml, classOf[FlowSchema])

    if (result.sources == null || result.sinks == null || result.detectors == null) {
      throw new IllegalArgumentException("flows.yaml must contain keys for sources, sinks, and detectors!")
    }

    result
  }
}
