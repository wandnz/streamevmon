package nz.net.wand.streamevmon.connectors.postgres

case class AsPathEntry(hopsInAs: Int, asNumber: AsNumber)

object AsPathEntry {
  def apply(entry: String): AsPathEntry = {
    val parts = entry.split('.')
    new AsPathEntry(parts(0).toInt, AsNumber(parts(1).toInt))
  }
}
