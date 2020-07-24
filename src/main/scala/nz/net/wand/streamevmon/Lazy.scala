package nz.net.wand.streamevmon

/** Simple wrapper class that lets us use proper lazy parameters.
  *
  * Regular parameters are `val`, and `: => T` parameters are `def`, meaning they are
  * re-evaluated on every call. There's no built-in equivalent of `lazy val`,
  * which evaluates only once and only when needed.
  *
  * @see [[https://stackoverflow.com/a/7187051]] for source
  */
class Lazy[A](operation: => A) {
  lazy val get: A = operation
}
