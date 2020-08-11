package nz.net.wand.streamevmon.flink.sources

import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.measurements.nab.NabMeasurement

import java.lang

import org.apache.flink.api.common.io.GenericCsvInputFormat
import org.apache.flink.core.fs.FileInputSplit

/** An InputFormat which parses data from the NAB dataset.
  *
  * @see [[https://github.com/numenta/NAB]]
  */
class NabFileInputFormat extends GenericCsvInputFormat[NabMeasurement]
                                 with HasFlinkConfig {

  // All the files have a header, which we don't care about.
  setSkipFirstLineAsHeader(true)

  override val flinkName: String = "NAB Measurement Source"
  override val flinkUid: String = "nab-source"
  override val configKeyGroup: String = "nab"

  private var currentStream: String = "UnknownNabStream"

  // We set the measurement's stream ID to the filename of the input file.
  private def setCurrentStream(split: FileInputSplit): Unit = {
    currentStream = s"${split.getPath.getParent.getName}/${split.getPath.getName}"
  }

  // When we open or reopen, we should make sure to set the stream ID.
  override def open(split: FileInputSplit): Unit = {
    super.open(split)
    setCurrentStream(split)
  }

  override def reopen(split: FileInputSplit, state: lang.Long): Unit = {
    super.reopen(split, state)
    setCurrentStream(split)
  }

  override def readRecord(
    reuse   : NabMeasurement,
    bytes   : Array[Byte],
    offset  : Int,
    numBytes: Int
  ): NabMeasurement = {
    // The measurement constructor handles splitting the input lines.
    NabMeasurement(
      currentStream,
      new String(bytes.slice(offset, offset + numBytes))
    )
  }
}
