package nz.net.wand.streamevmon

import java.io.{FileInputStream, InputStream}

import org.apache.flink.api.java.utils.ParameterTool
import org.snakeyaml.engine.v2.api.{Load, LoadSettings}

import scala.collection.JavaConverters._

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

  /** Flattens a Map[String, Map[String, T]\] to a Map[String, String], such
    * that nested maps are flattened into several dot-separated keys.
    *
    * The following two maps show an example input and output of this function.
    *
    * Map(
    * "topLevel" -> Map(
    * "secondLevel" -> true,
    * "thirdLevel" -> false
    * ),
    * "secondLevel" -> false
    * )
    *
    * Map(
    * "topLevel.secondLevel" -> true
    * "topLevel.thirdLevel" -> false
    * "secondLevel" -> false
    * )
    *
    * Supported values for the T field are: String, Map[String, T],
    * java.util.Map[String, T], java.util.List[T]. Java classes are
    * converted into Scala classes, then passed back into the function. Lists
    * are handled as though they are just several entries in the original map.
    */
  private def flattenMap[T](map: Map[String, T]): Map[String, String] = {
    map.flatMap {
      case (k, v) => v match {
        // If we're passed a Java map, just convert it to Scala and try again.
        // We really want to use the Scala map() function instead of messing
        // around with iterators and mutable holder buffers.
        case jMap: java.util.Map[String, T] => flattenMap(Map(k -> jMap.asScala.toMap))
        // If we're passed a Java list, it will most likely contain a series of
        // either additional maps, or leaves. Maps should be passed back in,
        // while leaves should be converted to String and returned.
        case jList: java.util.List[T] =>
          jList.asScala.flatMap {
            case item: java.util.Map[String, T] => flattenMap(Map(k -> item.asScala.toMap))
            case item: Map[String, T] => flattenMap(Map(k -> item))
            case leaf => Map(k -> leaf.toString)
          }
        // If we're given a map, just pass it back into the function. When we
        // hit a leaf, it will return a Map(deeperKey -> leaf). We want to
        // prepend the key at our current level to the deeper key, and pass it
        // back up the chain.
        case map: Map[String, T] =>
          flattenMap(map).map {
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
          loader.loadFromInputStream(yamlStream)
            .asInstanceOf[java.util.Map[String, Any]]
            .asScala.toMap
        )
          .asJava
      )
    }

    parameterToolFromYamlStream(
      getClass.getClassLoader.getResourceAsStream("generalSettings.yaml")
    )
      .mergeWith(
        parameterToolFromYamlStream(
          getClass.getClassLoader.getResourceAsStream("connectorSettings.yaml")
        )
      )
      .mergeWith(
        parameterToolFromYamlStream(
          getClass.getClassLoader.getResourceAsStream("detectorSettings.yaml")
        )
      )
      .mergeWith(
        parameterToolFromYamlStream(
          getClass.getClassLoader.getResourceAsStream("flowSettings.yaml")
        )
      )
      .mergeWith(
        parameterToolFromYamlStream(
          new FileInputStream("conf/streamevmon.yaml")
        )
      )
      .mergeWith(ParameterTool.fromSystemProperties())
      .mergeWith(ParameterTool.fromArgs(args))
  }
}
