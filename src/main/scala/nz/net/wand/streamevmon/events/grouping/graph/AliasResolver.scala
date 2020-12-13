package nz.net.wand.streamevmon.events.grouping.graph

import nz.net.wand.streamevmon.events.grouping.graph.itdk._

import java.io.File

import org.apache.flink.api.java.utils.ParameterTool

import scala.collection.mutable

class AliasResolver(
  itdkAliasLookup: Option[ItdkAliasLookup],
  itdkAsLookup   : Option[ItdkAsLookup],
  itdkGeoLookup  : Option[ItdkGeoLookup]
) extends Serializable {

  type HostT = Host2

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
    // Naive resolution.
    val naivelyMergedHost = mergedHosts.get(host.uid) match {
      // If we've seen this UID before, we know we need to merge it.
      case Some(oldMerged) =>
        // Do the merge, and notify the caller of an update.
        val newMerged = oldMerged.mergeWith(host)
        onUpdateHost(oldMerged, newMerged)
        newMerged
      case None =>
        // If we haven't seen it before, there's no merge to do. Notify the
        // caller of a new host.
        onNewHost(host)
        host
    }
    // Store the newly merged host back into the map. If it was previously
    // present, this will overwrite the old value.
    mergedHosts.put(host.uid, naivelyMergedHost)

    itdkAliasLookup match {
      case Some(value) => naivelyMergedHost
      case None => naivelyMergedHost
    }
  }
}

object AliasResolver {
  def apply(
    params             : ParameterTool,
    configKeyGroup     : String = "grouping.itdk",
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
        .map(aFile => new ItdkAliasLookup(new File(aFile), new File(s"$aFile.lookupHelper.json"))),
      Option(params.get(s"$configKeyGroup.asFile")).map(f => new ItdkAsLookup(new File(f))),
      Option(params.get(s"$configKeyGroup.geoFile")).map(f => new ItdkGeoLookup(new File(f)))
    )
  }
}
