package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.detectors.baseline.BaselineDetector
import nz.net.wand.streamevmon.detectors.changepoint.{ChangepointDetector, NormalDistribution}
import nz.net.wand.streamevmon.detectors.distdiff.{DistDiffDetector, WindowedDistDiffDetector}
import nz.net.wand.streamevmon.detectors.loss.LossDetector
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.detectors.spike.SpikeDetector
import nz.net.wand.streamevmon.flink._
import nz.net.wand.streamevmon.measurements.InfluxMeasurement
import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.bigdata.Flow

import java.time.Duration

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow

object UnifiedRunner extends UnifiedRunnerExtensions {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    System.setProperty("influx.dataSource.amp.subscriptionName", "AmpUnifiedRunner")
    System.setProperty("influx.dataSource.bigdata.subscriptionName", "BigDataUnifiedRunner")

    val config = Configuration.get(args)
    env.getConfig.setGlobalJobParameters(config)

    env.disableOperatorChaining

    env.enableCheckpointing(
      Duration.ofSeconds(config.getInt("flink.checkpointInterval")).toMillis,
      CheckpointingMode.EXACTLY_ONCE
    )

    lazy val ampMeasurementSource = env
      .addSource(new AmpMeasurementSourceFunction)
      .name("AMP Measurement Subscription")
      .uid("amp-measurement-source")

    lazy val icmpStream = ampMeasurementSource
      .filterType[ICMP]
      .notLossy[ICMP]
      .keyBy(keySelector)
    lazy val icmpStreamTimeWindow = icmpStream
      .timeWindow(Time.seconds(config.getInt("detector.default.windowDuration")))

    lazy val dnsStream = ampMeasurementSource
      .filterType[DNS]
      .keyBy(keySelector)
    lazy val dnsStreamTimeWindow = dnsStream
      .timeWindow(Time.seconds(config.getInt("detector.default.windowDuration")))

    lazy val bigdataMeasurementSource = env
      .addSource(new BigDataSourceFunction)
      .name("Libtrace-Bigdata Measurement Subscription")
      .uid("bigdata-measurement-source")

    lazy val flowStatisticStream = bigdataMeasurementSource
      .filterType[Flow]
      .keyBy(keySelector)
    lazy val flowStatisticStreamTimeWindow = flowStatisticStream
      .timeWindow(Time.seconds(config.getInt("detector.default.windowDuration")))

    if (isEnabled(env, "baseline")) {
      val baseline = new BaselineDetector[InfluxMeasurement]

      // TODO: Can we find a way to automatically wrap detectors if the config
      //  option is set? It would need to be aware of both the raw and windowed
      //  versions of the stream. I would also like to find a way to make
      //  the streams present without so much setup code up there, so that would
      //  tie in with that functionality more easily.
      if (config.getBoolean("detector.default.useFlinkTimeWindow")) {
        icmpStreamTimeWindow.wrapAndAddDetector(baseline)
      }
      else {
        icmpStream.addDetector(baseline)
      }
    }

    if (isEnabled(env, "changepoint")) {
      implicit val normalDistributionTypeInformation: TypeInformation[NormalDistribution[InfluxMeasurement]] =
        TypeInformation.of(classOf[NormalDistribution[InfluxMeasurement]])

      val changepoint = new ChangepointDetector[InfluxMeasurement, NormalDistribution[InfluxMeasurement]](
        new NormalDistribution(mean = 0)
      )
      if (config.getBoolean("detector.default.useFlinkTimeWindow")) {
        icmpStreamTimeWindow.wrapAndAddDetector(changepoint)
      }
      else {
        icmpStream.addDetector(changepoint)
      }
    }

    if (isEnabled(env, "distdiff")) {
      if (config.getBoolean("detector.distdiff.useFlinkWindow")) {
        val distDiffDetector = new WindowedDistDiffDetector[InfluxMeasurement, GlobalWindow]
        icmpStream
          .countWindow(config.getInt("detector.distdiff.recentsCount") * 2, 1)
          .addDetector(distDiffDetector)
      }
      else {
        val distDiffDetector = new DistDiffDetector[InfluxMeasurement]
        icmpStream.addDetector(distDiffDetector)
      }
    }

    if (isEnabled(env, "loss")) {
      val lossDetector = new LossDetector[InfluxMeasurement]
      if (config.getBoolean("detector.default.useFlinkTimeWindow")) {
        dnsStreamTimeWindow.wrapAndAddDetector(lossDetector)
      }
      else {
        dnsStream.addDetector(lossDetector)
      }
    }

    if (isEnabled(env, "mode")) {
      val modeDetector = new ModeDetector[InfluxMeasurement]
      if (config.getBoolean("detector.default.useFlinkTimeWindow")) {
        icmpStreamTimeWindow.wrapAndAddDetector(modeDetector)
      }
      else {
        icmpStream.addDetector(modeDetector)
      }
    }

    if (isEnabled(env, "spike")) {
      val spikeDetector = new SpikeDetector[InfluxMeasurement]
      if (config.getBoolean("detector.default.useFlinkTimeWindow")) {
        icmpStreamTimeWindow.wrapAndAddDetector(spikeDetector)
      }
      else {
        icmpStream.addDetector(spikeDetector)
      }
    }

    val allEvents = detectors.length match {
      case 0 => None
      case 1 => Some(detectors.head)
      // :_* just forces a sequence into the shape of varargs.
      case _ => Some(detectors.head.union(detectors.drop(1): _*))
    }

    val influxSink = new InfluxSinkFunction
    allEvents.map {
      _.addSink(influxSink)
        .setParallelism(1)
        .name("Influx Sink")
        .uid("influx-sink")
    }

    env.execute("Unified Runner")
  }
}
