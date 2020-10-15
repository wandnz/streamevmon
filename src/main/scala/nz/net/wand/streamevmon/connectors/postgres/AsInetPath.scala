package nz.net.wand.streamevmon.connectors.postgres

import nz.net.wand.streamevmon.measurements.amp.TracerouteMeta

/** Combines an AsPath and an InetPath, as though they were zipped. This ties
  * each InetAddress with its corresponding AsNumber, as reported by the data
  * source.
  */
case class AsInetPath(
  private val path: Iterable[AsInetPathEntry],
  meta            : TracerouteMeta
) extends Iterable[AsInetPathEntry] {
  override def iterator: Iterator[AsInetPathEntry] = path.iterator
}

object AsInetPath {
  def apply(
    inetPath: InetPath,
    asPath  : Option[AsPath],
    meta    : TracerouteMeta,
    distinguishMissingInetAddresses: Boolean
  ): AsInetPath = {
    AsInetPath(
      asPath match {
        case Some(asPathValue) =>
          var lastHop: AsInetPathEntry = null
          inetPath.zip(asPathValue.expandedPath).map { case (inet, asn) =>
            lastHop = if (inet == inetPath.head) {
              AsInetPathEntry(inet, asn, ampletHostname = Some(meta.source))
            }
            else {
              if (distinguishMissingInetAddresses) {
                AsInetPathEntry(inet, asn, lastHop = Some(lastHop))
              }
              else {
                AsInetPathEntry(inet, asn)
              }
            }
            lastHop
          }
        case None =>
          var lastHop: AsInetPathEntry = null
          inetPath.zip(
            Seq.fill(inetPath.size)(
              AsNumber(AsNumberCategory.Missing.id)
            )).map { case (inet, asn) =>
            lastHop = if (inet == inetPath.head) {
              AsInetPathEntry(inet, asn, ampletHostname = Some(meta.source))
            }
            else {
              if (distinguishMissingInetAddresses) {
                AsInetPathEntry(inet, asn, lastHop = Some(lastHop))
              }
              else {
                AsInetPathEntry(inet, asn)
              }
            }
            lastHop
          }
      },
      meta
    )
  }
}
