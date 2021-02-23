/* This file is part of streamevmon.
 *
 * Copyright (C) 2021  The University of Waikato, Hamilton, New Zealand
 *
 * Author: Daniel Oosterwijk
 *
 * All rights reserved.
 *
 * This code has been developed by the University of Waikato WAND
 * research group. For further information please see https://wand.nz,
 * or our Github organisation at https://github.com/wanduow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
object Configuration extends Logging {
  lazy val baseConfigDirectory: String = {
    if (new File("conf").exists) {
      "conf"
    }
    else {
      "/etc/streamevmon"
    }
  }

  /** Constructs a ParameterTool from a number of sources, each of which
    * override the previous.
    *
    * First, the default configuration is loaded.
    *
    * Second, the `./conf` directory is searched for files with the `.yml` or
    * `.yaml` extension. Any files that are not called `flows.yaml` or
    * `flows.yml` are loaded in lexicographical order. These files can be used
    * to provide environment-specific configuration options.
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
    * original map. This means that list type values are '''not supported''', but it
    * would be difficult to represent them in a flat Map anyway.
    */
  def flattenMap(map: Map[String, _]): Map[String, String] = {
    map.flatMap {
      case (k, v) => v match {
        // If we're passed a Java map, just convert it to Scala and try again.
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

  /** Function backing get(). */
  protected def getFromYaml(args: Array[String]): ParameterTool = {
    val loader = new Load(LoadSettings.builder().build())

    def parameterToolFromYamlStream(yamlStream: InputStream): ParameterTool = {
      // Yes, we convert to Scala collections for flattenMap, and then back to
      // Java again. The map() function is just too nice to pass up.
      ParameterTool.fromMap(
        flattenMap(
          Try(loader.loadFromInputStream(yamlStream))
            .toOption
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

    val customSettingsFiles = Option(new File(baseConfigDirectory).listFiles(
      (_: File, name: String) =>
        (name.endsWith(".yaml") || name.endsWith(".yml")) && (name != "flows.yaml" && name != "flows.yml")
    )).getOrElse(Array()).sorted.map { f =>
      logger.info(s"Loading configuration from ${f.getCanonicalPath}")
      new FileInputStream(f)
    }

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

  /** Gets a [[nz.net.wand.streamevmon.runners.unified.schema.FlowSchema FlowSchema]]
    * from a configuration file.
    *
    * If a text stream is provided as input, it is parsed as YAML.
    *
    * Otherwise, if the file `conf/flows.yaml` is present and contains valid
    * YAML, it is loaded.
    *
    * Otherwise, the internal default configuration is loaded.
    *
    * If the result does not contain keys titled `sources`, `flows`, and
    * `detectors`, an IllegalArgumentException is thrown.
    */
  def getFlowsDag(file: Option[InputStream] = None): FlowSchema = {
    val loader = new Load(LoadSettings.builder.build)
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

    val loadedYaml = file match {
      // Argument input stream
      case Some(value) =>
        logger.info(s"Loading flows definition from provided InputStream")
        loader.loadFromInputStream(value)
      // External user configuration
      case None => Try(loader.loadFromInputStream(
        new FileInputStream(new File(s"$baseConfigDirectory/flows.yaml"))
      )).toOption match {
        case None | Some(null) =>
          // Internal default configuration
          val defaults = loader.loadFromInputStream(
            getClass.getClassLoader.getResourceAsStream("flows.yaml")
          )
          logger.info("Loading flows definition from internal default configuration")
          defaults
        case Some(value) =>
          logger.info(s"Loading flows definition from $baseConfigDirectory/flows.yaml")
          value
      }
    }

    val result = mapper.convertValue(loadedYaml, classOf[FlowSchema])

    if (result.sources == null || result.sinks == null || result.detectors == null) {
      throw new IllegalArgumentException("flows.yaml must contain keys for sources, sinks, and detectors!")
    }

    result
  }
}
