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
  def apply(inetPath: InetPath, asPath: Option[AsPath], meta: TracerouteMeta): AsInetPath = {
    AsInetPath(
      asPath match {
        case Some(asPathValue) =>
          inetPath.zip(asPathValue.expandedPath).map { case (inet, asn) =>
            if (inet == inetPath.head) {
              AsInetPathEntry(inet, asn, Some(meta.source))
            }
            else {
              AsInetPathEntry(inet, asn)
            }
          }
        case None =>
          inetPath.zip(
            Seq.fill(inetPath.size)(
              AsNumber(AsNumberCategory.Missing.id)
            )).map { case (inet, asn) =>
            if (inet == inetPath.head) {
              AsInetPathEntry(inet, asn, Some(meta.source))
            }
            else {
              AsInetPathEntry(inet, asn)
            }
          }
      },
      meta
    )
  }
}
