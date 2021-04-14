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

package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.events.grouping.graph.itdk._

import java.io.File

import org.apache.flink.api.java.utils.ParameterTool

import scala.collection.mutable

/** Worker class for alias resolution, which finds duplicate entries for hosts.
  *
  * Note that the `mergedHosts` field has no way of pruning itself, and will
  * grow endlessly.
  *
  * ==Configuration==
  *
  * This class configures the construction of a new [[nz.net.wand.streamevmon.events.grouping.graph.itdk.ItdkAliasLookup ItdkAliasLookup]] object
  * if the constructor with a ParameterTool is used. As such, the default config
  * key group is `eventGrouping.graph.itdk`.
  *
  * - `alignedNodesFile`: If the file exists, the created AliasLookup will refer
  * to its contents.
  * - `nodesFile`: If the above file does not exist, but this one does, and the
  * `preprocessIfMissing` argument is set to true, then the file referred to by
  * this config element will be preprocessed, and its results used with a new
  * AliasLookup. This is likely to take a long time, so it's recommended to
  * preprocess these files separately using the entrypoint of
  * [[nz.net.wand.streamevmon.events.grouping.graph.itdk.ItdkLookupPreprocessor ItdkLookupPreprocessor]].
  *
  * @param itdkAliasLookup If provided, the ITDK dataset is used to perform more
  *                        advanced alias resolution.
  */
class AliasResolver(
  itdkAliasLookup: Option[ItdkAliasLookup]
) extends Serializable {

  type HostT = Host

  /** Keeps track of which hosts were replaced by merged hosts. The key is the
    * original host's UID, and the value is the merged host.
    */
  val mergedHosts: mutable.Map[String, HostT] = mutable.Map()

  /** Uses all available information to perform alias resolution. If no ITDK
    * data is provided, we do naive resolution based on the hostnames already
    * present in the Hosts supplied. If an
    * [[nz.net.wand.streamevmon.events.grouping.graph.itdk.ItdkAliasLookup ItdkAliasLookup]]
    * is provided, it is used to provide broader and more accurate aliases.
    *
    * Hostnames provided as part of the Hosts supplied is preferred over ITDK
    * data.
    */
  def resolve(
    host        : HostT,
    onNewHost   : HostT => Unit = _ => Unit,
    onUpdateHost: (HostT, HostT) => Unit = (_, _) => Unit
  ): HostT = {
    // Naive resolution. If we've seen it before, it'll be in the mergedHosts
    // map. We merge it with the new host to retain all the information. If we
    // haven't seen it before, just continue with the new host.
    val naivelyMergedHost = mergedHosts.get(host.uid).map(_.mergeWith(host)).getOrElse(host)

    // Now that we have all our current knowledge of the node, let's compare it
    // against the ITDK dataset.
    val withItdk = itdkAliasLookup match {
      case Some(itdk) if naivelyMergedHost.addresses.nonEmpty =>
        // Get all the unique node IDs that are present in the ITDK dataset for
        // the addresses in this host.
        val nodesAndAsns = naivelyMergedHost.addresses.flatMap { addr =>
          itdk.getNodeFromAddress(addr._1)
        }

        // If there's more than one, then our data disagrees with ITDK. This
        // hasn't yet been observed, so we don't handle it.
        if (nodesAndAsns.size > 1) {
          throw new IllegalStateException("Found multiple ITDK nodes for a single host")
        }

        // If we got a node ID, then one or more of the addresses is present in
        // the ITDK dataset.
        if (nodesAndAsns.size == 1) {
          // First, make sure that we don't end up with a single Host containing
          // addresses that ITDK believes are on different real host machines.
          // This hasn't yet been observed, so we don't handle it.
          if (naivelyMergedHost.itdkNodeId.isDefined && naivelyMergedHost.itdkNodeId == nodesAndAsns.headOption) {
            throw new IllegalStateException("Found contradicting ITDK nodes for a single host")
          }
          // If it doesn't contradict (including if this is the first time we got ITDK info),
          // we can happily continue with adding the new information.
          else {
            val naivelyMergedHostWithItdk = Host(
              naivelyMergedHost.hostnames,
              naivelyMergedHost.addresses,
              naivelyMergedHost.ampTracerouteUids,
              Some(nodesAndAsns.head)
            )
            // If we already have information on this ITDK node, we should merge
            // it with the new information. This step is only needed in the case
            // where `host` has no existing ITDK information, but a new address
            // is found in the ITDK dataset that's part of a node we already
            // have information for in a separate Host. We don't currently
            // detect this case, so we just do it all the time instead. This
            // is a good candidate for optimisation by improving the control
            // flow of this function.
            mergedHosts.get(naivelyMergedHostWithItdk.uid) match {
              case Some(value) =>
                val merged = value.mergeWith(naivelyMergedHostWithItdk)
                onUpdateHost(value, merged)
                merged
              case None => naivelyMergedHostWithItdk
            }
          }
        }
        // If we didn't find any ITDK information for the host, there's nothing
        // more to do.
        else {
          naivelyMergedHost
        }
      // If we don't have the ITDK dataset, or if we don't know any addresses
      // to compare against the dataset, we just use the naive merge method.
      case _ => naivelyMergedHost
    }

    // Finally, make sure that our mergedHosts map is up to date and that the
    // caller is notified of the changes we've made.
    val original = mergedHosts.remove(host.uid)
    mergedHosts.put(withItdk.uid, withItdk)
    original match {
      case Some(org) => onUpdateHost(org, withItdk)
      case None => onNewHost(withItdk)
    }
    withItdk
  }
}

object AliasResolver {
  def apply(
    params             : ParameterTool,
    configKeyGroup     : String = "eventGrouping.graph.itdk",
    preprocessIfMissing: Boolean = false
  ): AliasResolver = {
    val alignedFile = Option(params.get(s"$configKeyGroup.alignedNodesFile"))
    alignedFile match {
      case Some(aFile) if preprocessIfMissing && !new File(aFile).exists() =>
        Option(params.get(s"$configKeyGroup.nodesFile")) match {
          case Some(nFile) => ItdkLookupPreprocessor.preprocess(new File(nFile), cleanup = true)
          case None => throw new IllegalArgumentException(s"Preprocessing ITDK datafiles was requested, but no configuration entry for $configKeyGroup.nodesFile was found!")
        }
      case _ =>
    }

    new AliasResolver(
      Option(params.get(s"$configKeyGroup.alignedNodesFile"))
        .map(aFile => new ItdkAliasLookup(new File(aFile), new File(s"$aFile.lookupHelper.json")))
    )
  }
}
