package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.events.grouping.graph.itdk._

import java.io.File

import org.apache.flink.api.java.utils.ParameterTool

import scala.collection.mutable

class AliasResolver(
  itdkAliasLookup: Option[ItdkAliasLookup]
) extends Serializable {

  type HostT = Host

  val mergedHosts: mutable.Map[String, HostT] = mutable.Map()

  /** Uses all available information to perform alias resolution. If no ITDK
    * data is provided, we do naive resolution based on the hostnames already
    * present in the Hosts supplied. If an [[ItdkAliasLookup]] is provided,
    * it is used to provide broader and more accurate aliases.
    *
    * Hostnames provided as part of the Hosts supplied is preferred over ITDK
    * data.
    */
  def resolve(
    host        : HostT,
    onNewHost   : HostT => Unit,
    onUpdateHost: (HostT, HostT) => Unit
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
