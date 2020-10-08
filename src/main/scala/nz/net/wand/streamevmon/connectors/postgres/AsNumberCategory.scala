package nz.net.wand.streamevmon.connectors.postgres

object AsNumberCategory extends Enumeration {
  // RFC 1918 private address
  val PrivateAddress: Value = Value(-2)
  val Missing: Value = Value(-1)
  val Unknown: Value = Value(0)
  val Valid: Value = Value(1)
}
