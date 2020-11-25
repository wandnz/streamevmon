package nz.net.wand.streamevmon.connectors.postgres.schema

import nz.net.wand.streamevmon.measurements.amp.{Traceroute, TracerouteMeta}

/** Combines an AsPath and an InetPath, as though they were zipped. This ties
  * each InetAddress with its corresponding AsNumber, as reported by the data
  * source.
  */
case class AsInetPath(
  private val path: Iterable[AsInetPathEntry],
  measurement     : Traceroute,
  meta            : TracerouteMeta
) extends Iterable[AsInetPathEntry] {
  override def iterator: Iterator[AsInetPathEntry] = path.iterator
}

object AsInetPath {
  def apply(
    inetPath: InetPath,
    asPath: Option[AsPath],
    measurement        : Traceroute,
    meta: TracerouteMeta
  ): AsInetPath = {
    AsInetPath(
      asPath match {
        case Some(asPathValue) =>
          inetPath.zip(asPathValue.expandedPath).map { case (inet, asn) =>
            AsInetPathEntry(inet, asn)
          }
        case None =>
          inetPath.map { inet =>
            AsInetPathEntry(inet, AsNumber.Missing)
          }
      },
      measurement,
      meta
    )
  }
}
