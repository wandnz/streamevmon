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

package nz.net.wand.streamevmon.connectors

/** This package contains interfaces to InfluxDB which allow for subscription-
  * based and historical querying.
  *
  * ==Configuration==
  *
  * This module is configured by the `source.influx` config key group. This key
  * group allows for type-based overrides of configuration. For example, an
  * [[nz.net.wand.streamevmon.connectors.influx.InfluxConnection InfluxConnection]] or
  * [[nz.net.wand.streamevmon.connectors.influx.InfluxHistoryConnection InfluxHistoryConnection]]
  * which is constructed with a datatype of "amp" would get its configuration
  * first from the `source.influx.amp` key group. If a key is not found in that
  * group, it will look in the `source.influx` group.
  *
  * This allows multiple instances of the connectors to be configured separately
  * using the same ParameterTool, but also share certain configuration items.
  *
  * The following keys are shared between InfluxConnection and InfluxHistoryConnection:
  *
  * - `serverName`: The address that InfluxDB can be found at.
  * Default "localhost".
  *
  * - `portNumber`: The port that InfluxDB is listening on.
  * Default 8086.
  *
  * - `user`: The username that should be used to connect to InfluxDB.
  * Default "cuz"
  *
  * - `password`: The password that should be used to connect to InfluxDB.
  * Default "".
  *
  * - `databaseName`: The name of the InfluxDB database to subscribe to.
  * Default "nntsc".
  *
  * - `retentionPolicy`: The name of the InfluxDB retention policy to subscribe to.
  * Default "nntscdefault" for "amp" datatype, "autogen" otherwise.
  *
  * The following keys are only used in InfluxConnection:
  *
  * - `listenProtocol`: The transport protocol for this subscription. Can be one of
  * "http", "https", or "udp", although https and udp are unlikely to work.
  * Default "http".
  *
  * - `listenAddress`: The address to listen on for this subscription.
  * If not specified, this will be automatically generated at runtime by
  * inspecting the IP addresses attached to the interfaces on the host machine.
  * If a non-loopback, non-link-local address is found, the program will bind to
  * it. If several are found, it will prefer any that are not in restricted
  * private IP ranges. Specify this option if the automatic selection does not
  * fit your needs.
  *
  * - `listenPort`: The port this program should listen on.
  * Defaults to an ephemeral port if no configuration is supplied or if the desired port cannot be bound.
  *
  * - `listenBacklog`: The requested maximum length of the queue of incoming connections.
  * Default 5.
  * See [[https://docs.oracle.com/javase/8/docs/api/java/net/ServerSocket.html ServerSocket]] documentation.
  *
  * - `subscriptionName`: The name of the subscription that will be created in InfluxDB.
  * '''No default.''' This must be set explicitly to prevent conflicts.
  *
  */
package object influx {}
