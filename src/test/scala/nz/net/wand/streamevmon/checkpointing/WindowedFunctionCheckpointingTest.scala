package nz.net.wand.streamevmon.checkpointing

import nz.net.wand.streamevmon.detectors.distdiff.{DistDiffDetector, WindowedDistDiffDetector}
import nz.net.wand.streamevmon.detectors.WindowedFunctionWrapper
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.MockSink
import nz.net.wand.streamevmon.measurements.amp.ICMP

import java.time.{Duration, Instant}

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.{GlobalWindow, TimeWindow}

class WindowedFunctionCheckpointingTest extends NoHarnessCheckpointingTestBase {
  "Windowed Functions" should {
    "recover from checkpoints" when {
      "wrapping a KeyedProcessFunction" in {
        val expected = Event(
          "distdiff_events",
          "3",
          100,
          Instant.ofEpochMilli(1000000001002L),
          Duration.ofNanos(182000000L),
          "Distribution of ICMP has changed. Mean has increased from 1000.0 to 63437.5",
          Map("windowed" -> "false")
        )

        val env = getEnv
        val src = addFailingSource(env)

        src.window(TumblingEventTimeWindows.of(Time.seconds(1)))
          .process(new WindowedFunctionWrapper[ICMP, TimeWindow](
            new DistDiffDetector[ICMP]
          ))
          .addSink(new MockSink())

        env.execute("WindowedFunctionWrapperTest (DistDiff)")

        MockSink.received should contain(expected)
      }

      "type is WindowedDistDiffDetector" in {

        val expected = Event(
          "distdiff_events",
          "3",
          100,
          Instant.ofEpochMilli(1000000000630L),
          Duration.ofNanos(200000000L),
          "Distribution of ICMP has changed. Mean has increased from 1000.0 to 63437.5",
          Map("windowed" -> "true")
        )

        val env = getEnv
        val src = addFailingSource(env)

        src.countWindow(40, 1)
          .process(new WindowedDistDiffDetector[ICMP, GlobalWindow])
          .addSink(new MockSink())

        env.execute("WindowedDistDiffDetectorTest")

        MockSink.received should contain(expected)
      }
    }
  }
}
