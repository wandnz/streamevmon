package nz.net.wand.streamevmon

import java.nio.file.{Files, Paths}

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.databind.node.{ObjectNode, ValueNode}
import org.apache.flink.api.java.utils.ParameterTool
import org.snakeyaml.engine.v2.api.{Load, LoadSettings}

import scala.collection.JavaConverters._
import scala.collection.mutable

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
  def get(args: Array[String]): ParameterTool = {
    val defaults = ParameterTool.fromPropertiesFile(
      getClass.getClassLoader.getResourceAsStream("default.properties")
    )

    val withCustomFile = {
      val path = "conf/streamevmon.properties"
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

  def get(): ParameterTool = {
    get(Array())
  }

  /** This function does the same thing as [[flattenMap]], but works on the
    * response from the Jackson YAML parser. We ended out using SnakeYaml,
    * because it supports things like anchors more simply, but we'll leave this
    * here anyway, since it might come in useful later.
    *
    * @param mapper The ObjectMapper used to convert object types, so we can toString things.
    * @param node   The json node to convert.
    */
  private def jsonNodeToPropertiesMap(mapper: ObjectMapper, node: JsonNode): Map[String, String] = {
    node match {
      // ObjectNodes are collection types.
      case node: ObjectNode =>
        val result = mutable.Buffer[(String, String)]()

        // We'll convert all of the children into maps. If we were returned a map
        // with an empty key, then it was a leaf, and we should return a key
        // with no extra dots, as is appropriate. Otherwise, prepend the current
        // key onto the existing one.
        node.fields().forEachRemaining { entry =>
          jsonNodeToPropertiesMap(mapper, entry.getValue).foreach {
            case (k, v) =>
              result.append {
                (
                  k match {
                    case "" => entry.getKey
                    case _ => s"${entry.getKey}.$k"
                  },
                  v
                )
              }
          }
        }

        // It's much simpler to construct a buffer of tuples then convert it to
        // a map than to mutate a map on its own.
        result.toMap
      // This is a leaf node. The ObjectNode handler recognises the case of this
      // empty-key return value.
      case node: ValueNode =>
        Map("" -> mapper.convertValue(node, classOf[String]))
      // Anything else is unknown, and we don't know how to convert it.
      case node: JsonNode =>
        throw new NotImplementedError(s"Unknown node type ${node.getClass.getSimpleName}")
    }
  }

  /** Flattens a Map[String, Map[String, Any]\] to a Map[String, String], such
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
    * Supported values for the Any field are: String, Map[String, Any],
    * java.util.Map[String, Any], java.util.List[Any]. Java classes are
    * converted into Scala classes, then passed back into the function. Lists
    * are handled as though they are just several entries in the original map.
    */
  private def flattenMap(map: Map[String, Any]): Map[String, String] = {
    map.flatMap {
      case (k, v) => v match {
        // If we're passed a Java map, just convert it to Scala and try again.
        // We really want to use the Scala map() function instead of messing
        // around with iterators and mutable holder buffers.
        case jMap: java.util.Map[String, Any] => flattenMap(Map(k -> jMap.asScala.toMap))
        // If we're passed a Java list, it will most likely contain a series of
        // either additional maps, or leaves. Maps should be passed back in,
        // while leaves should be converted to String and returned.
        case jList: java.util.List[Any] =>
          jList.asScala.flatMap {
            case item: java.util.Map[String, Any] => flattenMap(Map(k -> item.asScala.toMap))
            case item: Map[String, Any] => flattenMap(Map(k -> item))
            case leaf => Map(k -> leaf.toString)
          }
        // If we're given a map, just pass it back into the function. When we
        // hit a leaf, it will return a Map(deeperKey -> leaf). We want to
        // prepend the key at our current level to the deeper key, and pass it
        // back up the chain.
        case map: Map[String, Any] =>
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

  def getFromYaml: ParameterTool = {
    val loader = new Load(LoadSettings.builder().build())

    val loaded = loader.loadFromInputStream(
      getClass.getClassLoader.getResourceAsStream("default.yaml")
    ).asInstanceOf[java.util.Map[String, Any]]

    // Yes, we convert to Scala collections for flattenMap, and then back to
    // Java again. The map() function is just too nice to pass up.
    ParameterTool.fromMap(flattenMap(loaded.asScala.toMap).asJava)
  }
}
