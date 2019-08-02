package nz.net.wand.amp.analyser

import nz.net.wand.amp.analyser.flink.RichMeasurementSourceFunction

import org.apache.flink.streaming.api.scala._

object StreamConsumer {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val sourceFunction = new RichMeasurementSourceFunction

    env.addSource(sourceFunction).print()

    env.execute()

    sys.exit()
  }
}
