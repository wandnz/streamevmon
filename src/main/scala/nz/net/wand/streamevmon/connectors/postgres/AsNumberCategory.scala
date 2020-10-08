package nz.net.wand.streamevmon.connectors.postgres

/** AMP can report a few special cases that aren't AS numbers.
  * RFC 1918 declares a few IP address ranges for private use, which
  * fall under PrivateAddress. Alternatively, discovering the AS
  * number could fail and result in a Missing or Unknown result.
  */
object AsNumberCategory extends Enumeration {
  // RFC 1918 private address
  val PrivateAddress: Value = Value(-2)
  val Missing: Value = Value(-1)
  val Unknown: Value = Value(0)
  val Valid: Value = Value(1)
}
