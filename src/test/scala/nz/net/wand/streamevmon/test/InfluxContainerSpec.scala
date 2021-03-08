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

package nz.net.wand.streamevmon.test

import nz.net.wand.streamevmon.connectors.influx.{InfluxConnection, InfluxHistoryConnection}
import nz.net.wand.streamevmon.flink.sinks.InfluxSinkFunction

import com.dimafeng.testcontainers.InfluxDBContainer
import com.github.fsanaulla.chronicler.ahc.management.InfluxMng
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.util.MockStreamingRuntimeContext

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class InfluxContainerSpec extends TaggedForAllTestContainer with TestBase {

  // We're turning off auth because we don't really care.
  override val container: InfluxDBContainer = InfluxDBContainer(tag = s"${InfluxDBContainer.defaultTag}-alpine", authEnabled = false)

  protected lazy val containerAddress = container.host
  protected lazy val containerPort = container.mappedPort(8086)

  override def afterStart(): Unit = {
    val influx = InfluxMng(containerAddress, containerPort, Some(InfluxCredentials(container.username, container.password)))

    Await.result(influx.updateRetentionPolicy(
      "autogen",
      container.database,
      duration = Some("0s")
    ),
      Duration.Inf)
  }

  protected def getInfluxSubscriber(
    subscriptionName: String,
    listenAddress: String = null
  ): InfluxConnection = {
    InfluxConnection(
      subscriptionName,
      container.database,
      "autogen",
      "http",
      if (listenAddress == null) {
        InfluxConnection.getListenAddress
      }
      else {
        listenAddress
      },
      0,
      5,
      containerAddress,
      containerPort,
      container.username,
      container.password
    )
  }

  protected def getInfluxHistory: InfluxHistoryConnection = {
    InfluxHistoryConnection(
      container.database,
      "autogen",
      "http",
      compress = false,
      containerAddress,
      containerPort,
      container.username,
      container.password
    )
  }

  /** @param listenAddress This defaults to null so that the ParameterTool
    *                      won't set the key.
    */
  protected def getInfluxConfigMap(
    subscriptionName: String,
    listenAddress: String = null
  ): Map[String, String] = {
    Map(
      "source.influx.subscriptionName" -> subscriptionName,
      "source.influx.databaseName" -> container.database,
      "source.influx.retentionPolicyName" -> "autogen",
      "source.influx.listenProtocol" -> "http",
      "source.influx.listenAddress" -> listenAddress,
      "source.influx.listenPort" -> "0",
      "source.influx.listenBacklog" -> "5",
      "source.influx.serverName" -> containerAddress,
      "source.influx.portNumber" -> containerPort.toString,
      "source.influx.user" -> container.username,
      "source.influx.password" -> container.password,
      "sink.influx.databaseName" -> container.database,
      "sink.influx.retentionPolicy" -> "autogen",
      "flink.maxLateness" -> "1"
    )
  }

  protected def getInfluxConfig(
    subscriptionName: String,
    listenAddress: String = null
  ): ParameterTool = {
    ParameterTool.fromMap(getInfluxConfigMap(subscriptionName, listenAddress).asJava)
  }

  protected def getSinkFunction(subscriptionName: String): InfluxSinkFunction = {
    val sink = new InfluxSinkFunction
    val context = new MockStreamingRuntimeContext(true, 1, 0)
    val params = getInfluxConfig(subscriptionName)
    context.getExecutionConfig.setGlobalJobParameters(params)
    sink.overrideConfig(params)
    sink.setRuntimeContext(context)
    sink
  }
}
