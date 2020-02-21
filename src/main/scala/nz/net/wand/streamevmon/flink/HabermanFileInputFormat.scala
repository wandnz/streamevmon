package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.measurements.haberman.{Haberman, SurvivalStatus}

import java.time.Instant

import org.apache.flink.api.common.io.GenericCsvInputFormat

class HabermanFileInputFormat(stream: Int) extends GenericCsvInputFormat[Haberman] {

  override def openInputFormat(): Unit = {
    if (getRuntimeContext.getNumberOfParallelSubtasks > 1) {
      throw new IllegalStateException("Parallelism for this InputFormat must be 1.")
    }
  }

  private var lineCounter = -1

  override def readRecord(
    reuse: Haberman,
    bytes: Array[Byte],
    offset: Int,
    numBytes: Int
  ): Haberman = {
    val line = new String(bytes.slice(offset, offset + numBytes))
    val parts = line.split(",")
    lineCounter += 1
    Haberman(
      stream,
      parts(0).toInt,
      parts(1).toInt + 1900,
      parts(2).toInt,
      parts(3) match {
        case "1" => SurvivalStatus.LessThan5Years
        case "2" => SurvivalStatus.MoreThan5Years
        case _ => throw new IllegalArgumentException(s"Invalid entry: $line")
      },
      Instant.ofEpochMilli(1000000000000L + lineCounter)
    )
  }
}
