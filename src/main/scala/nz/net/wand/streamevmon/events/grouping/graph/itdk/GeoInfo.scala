package nz.net.wand.streamevmon.events.grouping.graph.itdk

case class GeoInfo(
  nodeId   : Long,
  continent: String,
  country  : String,
  region   : String,
  city     : String,
  latitude : Float,
  longitude: Float
)

object GeoInfo {
  def apply(line: String): GeoInfo = {
    val parts = line.split("\t")
    GeoInfo(
      parts(0).drop(10).dropRight(1).toInt,
      parts(1),
      parts(2),
      parts(3),
      parts(4),
      parts(5).toFloat,
      parts(6).toFloat
    )
  }
}
