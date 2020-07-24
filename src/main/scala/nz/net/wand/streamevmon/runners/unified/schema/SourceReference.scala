package nz.net.wand.streamevmon.runners.unified.schema

import com.fasterxml.jackson.module.scala.JsonScalaEnumeration

/** Refers to a particular stream from a particular source.
  *
  * @param name        The source's user-defined name. This is the key in the [[FlowSchema]]'s
  *                    `sources` field which points to the desired SourceInstance.
  * @param datatype    The datatype desired from the source.
  *                    // TODO: It is possible for this datatype to not be supported by the source.
  * @param filterLossy Whether or not to remove lossy measurements from the typed stream.
  */
case class SourceReference(
  name : String,
  @JsonScalaEnumeration(classOf[SourceDatatypeReference])
  datatype: SourceDatatype.Value,
  filterLossy: Boolean
)
