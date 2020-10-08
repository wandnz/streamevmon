package nz.net.wand.streamevmon.connectors.postgres

case class AsNumber(private val num: Int) {
  // We could use AsNumberCategory(num), but this would throw
  // an exception for all valid ASes except ID 1. We want to
  // avoid the cost involved with an exception.
  val category: AsNumberCategory.Value = num match {
    case -2 => AsNumberCategory.PrivateAddress
    case -1 => AsNumberCategory.Missing
    case 0 => AsNumberCategory.Unknown
    case _ => AsNumberCategory.Valid
  }

  val number: Option[Int] = category match {
    case AsNumberCategory.Valid => Some(num)
    case _ => None
  }

  override def toString: String = category match {
    case AsNumberCategory.Unknown => "Unknown"
    case AsNumberCategory.Missing => "Missing"
    case AsNumberCategory.PrivateAddress => "Private"
    case AsNumberCategory.Valid => s"AS $num"
  }
}
