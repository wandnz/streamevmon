package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.flink.InfluxSubscriptionSourceFunction
import nz.net.wand.streamevmon.measurements._
import nz.net.wand.streamevmon.Configuration

import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.util.Collector

object LossDetector {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    System.setProperty("influx.dataSource.subscriptionName", "LossDetector")

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    env.disableOperatorChaining

    val sourceFunction = new InfluxSubscriptionSourceFunction[String] {
      override protected[this] def processLine(ctx: SourceFunction.SourceContext[String],
                                               line: String): Option[String] = Some(line)
    }

    val unparsedOutputTag: OutputTag[String] = OutputTag[String]("unparsed-measurements")
    val parseFunction = new ProcessFunction[String, Measurement] {
      override def processElement(value: String,
                                  ctx: ProcessFunction[String, Measurement]#Context,
                                  out: Collector[Measurement]): Unit = {
        val result = MeasurementFactory.createMeasurement(value)
        result match {
          case Some(x) => out.collect(x)
          case None => ctx.output(unparsedOutputTag, value)
        }
      }
    }

    val processFunction = new ProcessFunction[Measurement, Measurement] {
      override def processElement(
          value: Measurement,
          ctx: ProcessFunction[Measurement, Measurement]#Context,
          out: Collector[Measurement]
      ): Unit = {
        value match {
          case d: DNS        => if (d.lossrate > 0) out.collect(d)
          case _: HTTP       => // Don't see any way to measure loss with HTTP
          case i: ICMP       => if (i.loss > 0) out.collect(i)
          case t: TCPPing    => if (t.loss > 0) out.collect(t)
          case _: Traceroute => // No way to measure loss here either
          case _             => // Shouldn't get any other cases
        }
      }
    }

    val inputStream = env
      .addSource(sourceFunction)
      .name("InfluxDB Subscription Line Source")

    val parsedStream = inputStream
      .process(parseFunction)
      .name("Line Protocol -> Measurement converter with side output")

    val eventStream = parsedStream
      .process(processFunction)
      .name("Loss Detection Function")

    parsedStream.getSideOutput(unparsedOutputTag).print("Unparsed Measurements")
    eventStream.print("Lossy Measurements")

    env.execute("Loss Detector")
  }
}
