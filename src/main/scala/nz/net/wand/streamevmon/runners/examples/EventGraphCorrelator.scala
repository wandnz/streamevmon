package nz.net.wand.streamevmon.runners.examples

import nz.net.wand.streamevmon.Configuration
import nz.net.wand.streamevmon.flink.sources.{AmpMeasurementSourceFunction, PostgresTracerouteSourceFunction}

import java.time.Duration

import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.scala._

object EventGraphCorrelator {
  def main(args: Array[String]): Unit = {
    val time = System.currentTimeMillis()

    // First, set up a StreamExecutionEnvironment.
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.enableCheckpointing(10000, CheckpointingMode.EXACTLY_ONCE)
    env.setRestartStrategy(RestartStrategies.noRestart())
    env.setParallelism(1)

    env.getConfig.setGlobalJobParameters(Configuration.get(args))

    // We need some deterministic streams that will generate events.
    // There also needs to be some Traceroute measurements so that we can
    // actually build the graph.
    // We'll try the cauldron-collector dataset.

    // Just get all the history :)
    val sourceFunction = new AmpMeasurementSourceFunction(
      fetchHistory = Duration.ofMillis(System.currentTimeMillis())
    )

    //val source = env
    //  .addSource(sourceFunction)
    //  .name("Measurement history and subscription")

    val pgCon = PostgresConnection(
      "localhost",
      5432,
      "nntsc",
      "cuz",
      "",
      0
    )

    val metaExtractor = new MeasurementMetaExtractor[InfluxMeasurement]

    //val withMetaExtractor = source.process(metaExtractor).name("Meta extractor")
    //val metas = withMetaExtractor.getSideOutput(metaExtractor.outputTag)

    //withMetaExtractor.print("Measurements")
    //metas.print("Metas")
    //metas.addSink(_ => Unit)

    val pgSource = env
      .addSource(new PostgresTracerouteSourceFunction(
        fetchHistory = Duration.ofDays(365)
      ))

    pgSource.addSink(_ => Unit)

    // Next, set up all the detectors to use. We might as well use all of them
    // with their default settings, since it doesn't really matter how good the
    // accuracy is for this test implementation. We just want event volume.

    // Now that we have a number of event streams, we need to tie them all
    // together and pass them to our correlator. We might as well print them too.

    env.execute()

    val time2 = System.currentTimeMillis()
    println(time2 - time, "s")
  }
}
