package nz.ac.waikato

import org.apache.flink.streaming.api.scala._

object StreamConsumer
{

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    env.addSource(new ICMPMeasurementSourceFunction()).print()

    env.execute()
  }
}
