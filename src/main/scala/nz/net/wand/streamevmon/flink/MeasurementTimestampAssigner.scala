package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.measurements.Measurement

import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner

class MeasurementTimestampAssigner extends SerializableTimestampAssigner[Measurement] {
  override def extractTimestamp(element: Measurement, recordTimestamp: Long): Long = element.time.toEpochMilli
}
