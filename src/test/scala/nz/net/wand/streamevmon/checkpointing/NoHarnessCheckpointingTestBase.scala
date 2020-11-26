package nz.net.wand.streamevmon.checkpointing

import nz.net.wand.streamevmon.{Configuration, TestBase}
import nz.net.wand.streamevmon.flink.FailingSource
import nz.net.wand.streamevmon.measurements.amp.ICMP
import nz.net.wand.streamevmon.measurements.MeasurementKeySelector

import java.time.Duration

import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

trait NoHarnessCheckpointingTestBase extends TestBase {
  val keySelector: MeasurementKeySelector[ICMP] = new MeasurementKeySelector[ICMP]

  def getEnv: StreamExecutionEnvironment = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val config = Configuration.get(Array())
    env.getConfig.setGlobalJobParameters(config)

    env.disableOperatorChaining

    env.setParallelism(1)

    env.enableCheckpointing(
      Duration.ofSeconds(1).toMillis,
      CheckpointingMode.EXACTLY_ONCE
    )

    env.setRestartStrategy(RestartStrategies.fixedDelayRestart(10, Time.seconds(1).toMilliseconds))

    env
  }

  def addFailingSource(env: StreamExecutionEnvironment): KeyedStream[ICMP, String] = {
    env.addSource(new FailingSource)
      .name("Failing Source")
      .uid("failing-source")
      .keyBy(keySelector)
  }
}
