package nz.net.wand.streamevmon.events.grouping.graph.itdk

import nz.net.wand.streamevmon.connectors.postgres.schema.AsNumber

import scala.util.Try

/** An AS number obtained from an ITDK dataset. We retain the node ID and method
  * because they're there in the file, but we don't really need either of them.
  */
class ItdkAsNumber(
  val nodeId     : Long,
  private val num: Int,
  val method     : Option[ItdkAsMethod.Value]
) extends AsNumber(num)

object ItdkAsNumber {
  def apply(line: String): ItdkAsNumber = {
    val parts = line.split(" ")
    new ItdkAsNumber(
      parts(1).drop(1).toInt,
      parts(2).toInt,
      if (parts.size > 3) {
        Try(ItdkAsMethod.withName(parts(3))).toOption
      }
      else {
        None
      }
    )
  }
}

object ItdkAsMethod extends Enumeration {
  val Interfaces: Value = Value("interfaces")
  val Refinement: Value = Value("refinement")
  val LastHop: Value = Value("last_hop")
}
