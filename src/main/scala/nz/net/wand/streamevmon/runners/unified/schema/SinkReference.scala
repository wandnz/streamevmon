package nz.net.wand.streamevmon.runners.unified.schema

/** Just refers to a particular sink. This could be a plain string, but I'd like
  * it to be a bit clearer and more expandable.
  */
case class SinkReference(
  name: String
)
