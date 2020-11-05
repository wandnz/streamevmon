package nz.net.wand.streamevmon.connectors.postgres

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
    meta               : TracerouteMeta,
    distinguishMissingInetAddresses: Boolean,
    compressMissingInetChains      : Boolean
  ): AsInetPath = {
    AsInetPath(
      asPath match {
        case Some(asPathValue) =>
          var lastHop: AsInetPathEntry = null
          inetPath.zip(asPathValue.expandedPath).map { case (inet, asn) =>
            val `h` = inetPath.head
            val `t` = inetPath.last
            val hostname = inet match {
              case _@`h` => Some(meta.source)
              case _@`t` => Some(meta.destination)
              case _ => None
            }

            val result = if (distinguishMissingInetAddresses) {
              AsInetPathEntry(inet, asn, lastHop = Some(lastHop))
            }
            else {
              AsInetPathEntry(inet, asn)
            }
            if (!compressMissingInetChains || result.address.isDefined) {
              lastHop = result
            }
            result
          }
        case None =>
          var lastHop: AsInetPathEntry = null
          inetPath.zip(
            Seq.fill(inetPath.size)(
              AsNumber(AsNumberCategory.Missing.id)
            )).map { case (inet, asn) =>
            val `h` = inetPath.head
            val `t` = inetPath.last
            val hostname = inet match {
              case _@`h` => Some(meta.source)
              case _@`t` => Some(meta.destination)
              case _ => None
            }

            val result = if (distinguishMissingInetAddresses) {
              AsInetPathEntry(inet, asn, lastHop = Some(lastHop))
            }
            else {
              AsInetPathEntry(inet, asn)
            }
            if (!compressMissingInetChains || result.address.isDefined) {
              lastHop = result
            }
            result
          }
      },
      measurement,
      meta
    )
  }
}
