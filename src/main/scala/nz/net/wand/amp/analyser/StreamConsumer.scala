package nz.net.wand.amp.analyser

import org.apache.flink.streaming.api.scala._

object StreamConsumer {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    env.addSource(new MeasurementSourceFunction()).print()

    env.execute()

    sys.exit()
  }
}
