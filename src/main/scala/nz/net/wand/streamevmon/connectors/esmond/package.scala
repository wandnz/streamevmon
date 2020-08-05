package nz.net.wand.streamevmon.connectors

/** Contains the API interface for ESnet perfSONAR esmond archives.
  *
  * You should only need to use
  * [[nz.net.wand.streamevmon.connectors.esmond.EsmondConnectionForeground EsmondConnectionForeground]].
  * Documentation for each of the API-calling functions in that class can be found in
  * [[nz.net.wand.streamevmon.connectors.esmond.EsmondAPI EsmondAPI]].
  *
  * See [[nz.net.wand.streamevmon.connectors.esmond.EsmondStreamDiscovery EsmondStreamDiscovery]] for configuration.
  *
  * Schema objects can be used to build [[nz.net.wand.streamevmon.measurements.esmond measurements]].
  *
  * @see [[https://docs.perfsonar.net/esmond_api_rest.html]]
  */
package object esmond {}
