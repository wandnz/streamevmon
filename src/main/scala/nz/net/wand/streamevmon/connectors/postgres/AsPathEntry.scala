package nz.net.wand.streamevmon.connectors.postgres

/** This is part of an [[AsPath]]. `hopsInAs` is the number of
  * separate addresses which were visited before leaving the AS described
  * in `asNumber`.
  */
case class AsPathEntry(hopsInAs: Int, asNumber: AsNumber)

object AsPathEntry {
  /** Interprets the format from AMP's PostgreSQL database. */
  def apply(entry: String): AsPathEntry = {
    val parts = entry.split('.')
    new AsPathEntry(parts(0).toInt, AsNumber(parts(1).toInt))
  }
}
