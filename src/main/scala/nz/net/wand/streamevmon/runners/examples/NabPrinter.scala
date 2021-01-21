package nz.net.wand.streamevmon.runners.examples

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.flink.sources.NabFileInputFormat

import org.apache.flink.core.fs.Path
import org.apache.flink.streaming.api.scala._

/** Prints out the entire NAB dataset. This is mostly just a test to make sure
  * the parsers don't crash while reading any of the input files.
  *
  * @see [[https://github.com/numenta/NAB]]
  */
object NabPrinter {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)

    val config = Configuration.get(args)
    env.getConfig.setGlobalJobParameters(config)

    val format = new NabFileInputFormat
    // We want to read all the files in all the subfolders
    format.setNestedFileEnumeration(true)
    // But we don't want README.md.
    format.setFilesFilter((filePath: Path) => {
      filePath.getPath.endsWith(".md")
    })

    env
      .readFile(
        format,
        "./data/NAB/data"
      )
      .print()

    env.execute()
  }
}
