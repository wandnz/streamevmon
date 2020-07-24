package nz.net.wand.streamevmon.runners.unified.schema

import com.fasterxml.jackson.core.`type`.TypeReference

// Jackson gets confused about scala enums if we don't include an annotation
// with one of these classes where we expect it.

private class DetectorTypeReference extends TypeReference[DetectorType.type]

private class SinkTypeReference extends TypeReference[SinkType.type]

private class SourceTypeReference extends TypeReference[SourceType.type]

private class SourceSubtypeReference extends TypeReference[SourceSubtype.type]

private class SourceDatatypeReference extends TypeReference[SourceDatatype.type]
