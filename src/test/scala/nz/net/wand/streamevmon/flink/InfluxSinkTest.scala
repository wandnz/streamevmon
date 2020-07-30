package nz.net.wand.streamevmon.flink

import nz.net.wand.streamevmon.{InfluxContainerSpec, SeedData}
import nz.net.wand.streamevmon.events.Event

import java.time.{Instant, Duration => JavaDuration}

import com.github.fsanaulla.chronicler.ahc.io.InfluxIO
import com.github.fsanaulla.chronicler.core.model.InfluxCredentials

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration => ScalaDuration}

class InfluxSinkTest extends InfluxContainerSpec {
  "InfluxSink" should {
    "write data to InfluxDB" in {
      val sink = getSinkFunction("no-subscription")
      sink.open(null)

      sink.invoke(SeedData.event.withTags, new MockSinkContext(SeedData.event.withTags.time))
      sink.invoke(SeedData.event.withoutTags, new MockSinkContext(SeedData.event.withoutTags.time))

      sink.close()

      val db = InfluxIO(containerAddress, containerPort, Some(InfluxCredentials(container.username, container.password)))
        .database(container.database)

      Await.result(
        db.readJson(
          s"SELECT time,stream,severity,detection_latency,description FROM ${SeedData.event.withoutTags.eventType}")
          .map(e => {
            if (e.isLeft) {
              fail(e.left.get)
            }
            val arr = e.right.get.head

            Event(
              eventType = SeedData.event.withoutTags.eventType,
              stream = arr.get(1).toString.drop(1).dropRight(1).toInt,
              severity = arr.get(2).asInt,
              time = Instant.parse(arr.get(0).toString.drop(1).dropRight(1)),
              detectionLatency = JavaDuration.ofNanos(arr.get(3).asInt),
              description = arr.get(4).asString,
              tags = Map()
            ) shouldBe SeedData.event.withoutTags
          }),
        ScalaDuration.Inf
      )

      Await.result(
        db.readJson(
          s"SELECT time,type,secondTag,stream,severity,detection_latency,description FROM ${SeedData.event.withTags.eventType}")
          .map(e => {
            if (e.isLeft) {
              fail(e.left.get)
            }
            val arr = e.right.get.head

            Event(
              eventType = SeedData.event.withTags.eventType,
              stream = arr.get(3).toString.drop(1).dropRight(1).toInt,
              severity = arr.get(4).asInt,
              time = Instant.parse(arr.get(0).toString.drop(1).dropRight(1)),
              detectionLatency = JavaDuration.ofNanos(arr.get(5).asInt),
              description = arr.get(6).asString,
              tags = Map(
                "type" -> arr.get(1).asString,
                "secondTag" -> arr.get(2).asString
              )
            ) shouldBe SeedData.event.withTags
          }),
        ScalaDuration.Inf
      )
    }
  }
}
