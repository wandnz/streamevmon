package nz.net.wand.streamevmon.connectors

import nz.net.wand.streamevmon.events.{ChangepointEvent, Event, ThresholdEvent}
import nz.net.wand.streamevmon.flink.InfluxSinkFunction
import nz.net.wand.streamevmon.SeedData

import java.time.Instant

import com.github.fsanaulla.chronicler.ahc.io.InfluxIO

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

class InfluxSinkTest extends InfluxContainerSpec {
  "InfluxSink" should {
    "write data to InfluxDB" in {
      val sink = new InfluxSinkFunction[Event]
      sink.overrideConfig(getInfluxConfig("no-subscription"))
      sink.open(null)

      sink.invoke(SeedData.thresholdEvent.withTags)
      sink.invoke(SeedData.changepointEvent.withTags)

      sink.close()

      val db = InfluxIO(container.address, container.port, Some(container.credentials))
        .database(container.database)

      Await.result(
        db.readJson(
          s"SELECT time,moreTags,type,stream,severity,detection_latency,description FROM ${ChangepointEvent.measurementName}")
          .map(e => {
            if (e.isLeft) {
              println(e)
              fail("No data was placed in InfluxDB")
            }
            val arr = e.right.get.head
            assert(
              ChangepointEvent(
                Map(
                  "moreTags" -> arr.get(1).toString.drop(1).dropRight(1),
                  "type" -> arr.get(2).toString.drop(1).dropRight(1)
                ),
                arr.get(3).toString.drop(1).dropRight(1).toInt,
                arr.get(4).asInt,
                Instant.parse(arr.get(0).toString.drop(1).dropRight(1)),
                arr.get(5).asInt,
                arr.get(6).toString.drop(1).dropRight(1)
              )
                === SeedData.changepointEvent.withTags
            )
          }),
        Duration.Inf
      )

      Await.result(
        db.readJson(
          s"SELECT time,stream,type,severity,detection_latency,description FROM ${ThresholdEvent.measurementName}")
          .map(e => {
            if (e.isLeft) {
              println(e)
              fail("No data was placed in InfluxDB")
            }
            val arr = e.right.get.head
            assert(
              ThresholdEvent(
                Map(
                  "stream" -> arr.get(1).toString.drop(1).dropRight(1),
                  "type" -> arr.get(2).toString.drop(1).dropRight(1)
                ),
                arr.get(3).asInt,
                Instant.parse(arr.get(0).toString.drop(1).dropRight(1)),
                arr.get(4).asInt,
                arr.get(5).toString.drop(1).dropRight(1)
              )
                === SeedData.thresholdEvent.withTags
            )
          }),
        Duration.Inf
      )
    }
  }
}
