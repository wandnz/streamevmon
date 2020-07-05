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
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow

object UnifiedRunner extends UnifiedRunnerExtensions {
  def main(args: Array[String]): Unit = {
    // == Setup config ==
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    System.setProperty("influx.dataSource.amp.subscriptionName", "AmpUnifiedRunner")
    System.setProperty("influx.dataSource.bigdata.subscriptionName", "BigDataUnifiedRunner")

    config = Configuration.get(args)
    env.getConfig.setGlobalJobParameters(config)

    env.disableOperatorChaining

    env.enableCheckpointing(
      Duration.ofSeconds(config.getInt("flink.checkpointInterval")).toMillis,
      CheckpointingMode.EXACTLY_ONCE
    )

    // == Create input streams ==
    lazy val ampMeasurementSource = env
      .addSource(new AmpMeasurementSourceFunction)
      .name("AMP Measurement Subscription")
      .uid("amp-measurement-source")

    lazy val filteredIcmp = ampMeasurementSource
      .filterType[ICMP]
      .notLossy[ICMP]

    lazy val icmpStreams = filteredIcmp
      .keyAndTimeWindowStreams

    lazy val dnsStreams = ampMeasurementSource
      .filterType[DNS]
      .keyAndTimeWindowStreams

    lazy val bigdataMeasurementSource = env
      .addSource(new BigDataSourceFunction)
      .name("Libtrace-Bigdata Measurement Subscription")
      .uid("bigdata-measurement-source")

    lazy val flowStatisticStreams = bigdataMeasurementSource
      .filterType[Flow]
      .keyAndTimeWindowStreams

    // == Create detectors ==

    // Simple requirements
    icmpStreams.addDetector(new BaselineDetector[InfluxMeasurement])
    dnsStreams.addDetector(new LossDetector[InfluxMeasurement])
    icmpStreams.addDetector(new ModeDetector[InfluxMeasurement])
    icmpStreams.addDetector(new SpikeDetector[InfluxMeasurement])

    // Changepoint detector needs an extra TypeInformation, but is otherwise simple
    implicit val normalDistributionTypeInformation: TypeInformation[NormalDistribution[InfluxMeasurement]] =
      TypeInformation.of(classOf[NormalDistribution[InfluxMeasurement]])
    icmpStreams.addDetector(new ChangepointDetector[InfluxMeasurement, NormalDistribution[InfluxMeasurement]](
      new NormalDistribution(mean = 0)
    ))

    // DistDiff detector implements windowing manually, so needs its own window operator
    filteredIcmp
      .keyAndCountWindowStreams(config.getInt("detector.distdiff.recentsCount") * 2, 1)
      .addDetector(new DistDiffDetector[InfluxMeasurement], new WindowedDistDiffDetector[InfluxMeasurement, GlobalWindow])

    // == Tie detectors into sink ==

    val allEvents = detectors.length match {
      case 0 => None
      case 1 => Some(detectors.head)
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
