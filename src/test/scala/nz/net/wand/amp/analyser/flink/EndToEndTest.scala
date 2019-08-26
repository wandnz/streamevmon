package nz.net.wand.amp.analyser.flink

import nz.net.wand.amp.analyser.SeedData
import nz.net.wand.amp.analyser.connectors.InfluxContainerSpec
import nz.net.wand.amp.analyser.events.{Event, ThresholdEvent}
import nz.net.wand.amp.analyser.measurements.Measurement

import com.github.fsanaulla.chronicler.ahc.io.InfluxIO
import jawn.ast.{JArray, JNum, JString}
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.function.ProcessAllWindowFunction
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.language.postfixOps

class EndToEndTest extends InfluxContainerSpec {

  private[this] lazy val ourClassLoader: ClassLoader = this.getClass.getClassLoader
  private[this] lazy val actorSystem =
    akka.actor.ActorSystem(getClass.getSimpleName, classLoader = Some(ourClassLoader))

  private[this] val expected = Array(
    JArray(
      Array(
        JString(SeedData.icmp.expected.time.toString),
        JNum(5),
        JString("3")
      )),
    JArray(
      Array(
        JString(SeedData.dns.expected.time.toString),
        JNum(5),
        JString("1")
      )),
    JArray(
      Array(
        JString(SeedData.traceroute.expected.time.toString),
        JNum(5),
        JString("5")
      ))
  )

  // TODO: This test is pretty janky and not 100% reliable.
  // It'll work just fine if the pipeline runs in a reasonable amount of time,
  // but it fails if it takes too long. Can't figure out a way to pass data between
  // this code and the code running in the pipeline, and it would likely require
  // special test-only code in the pipeline side of things, which isn't pretty.
  "Program pipeline" should {
    "receive, process, and output data" in {
      // Set up execution environment
      val env = StreamExecutionEnvironment.getExecutionEnvironment
      env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

      env.getConfig.setGlobalJobParameters(getInfluxConfig("EndToEndTest", "130.217.250.59"))

      val sourceFunction = new MeasurementSubscriptionSourceFunction
      val sinkFunction = getSinkFunction

      val processFunction: ProcessAllWindowFunction[Measurement, Event, TimeWindow] =
        new ProcessAllWindowFunction[Measurement, Event, TimeWindow] {
          override def process(context: Context,
                               elements: Iterable[Measurement],
                               out: Collector[Event]): Unit = {

            def processFunc(elements: Iterable[Measurement]): Iterable[ThresholdEvent] = {
              elements.map { e =>
                ThresholdEvent(
                  tags = Map(
                    "stream" -> e.stream.toString
                  ),
                  severity = 5,
                  time = e.time
                )
              }
            }

            processFunc(elements).foreach(out.collect)
          }
        }

      val processed = env
        .addSource(sourceFunction)
        .windowAll(TumblingEventTimeWindows.of(Time.seconds(5)))
        .process(processFunction)

      //processed.print()
      processed.addSink(sinkFunction)

      // Spin off the Flink job
      actorSystem.scheduler.scheduleOnce(0 seconds)(env.execute())

      // Send some data to InfluxDB to be received by the Flink job after some time to let it start
      val db = InfluxIO(container.address, container.port, Some(container.credentials))
        .database(container.database)

      Thread.sleep(2000)
      println("Sending data")
      Await.result(db.writeNative(SeedData.icmp.subscriptionLine), Duration.Inf)
      Thread.sleep(20)
      Await.result(db.writeNative(SeedData.dns.subscriptionLine), Duration.Inf)
      Thread.sleep(20)
      Await.result(db.writeNative(SeedData.traceroute.subscriptionLine), Duration.Inf)
      println("Data sent.")
      Thread.sleep(2000)

      sourceFunction.shutdownAll()

      // Check that the data is in InfluxDB after giving it a bit of time to get there.
      Thread.sleep(1000)

      Await.result(
        db.readJson(s"SELECT * FROM ${ThresholdEvent.measurement_name}")
          .map(e => {
            if (e.isLeft) {
              println(e)
              fail("No data was placed in InfluxDB")
            }
            assert(e.right.get === expected)
          }),
        Duration.Inf
      )
    }
  }


  "MeasurementSubscriptionSourceFunction" should {

    def sendData(andThen: () => Any = () => Unit): Unit = {
      val db = InfluxIO(container.address, container.port, Some(container.credentials))
        .database(container.database)

      actorSystem.scheduler
        .scheduleOnce(20 millis) {
          println("Sending data")
          Await.result(db.writeNative(SeedData.icmp.subscriptionLine), Duration.Inf)
          Thread.sleep(20)
          Await.result(db.writeNative(SeedData.dns.subscriptionLine), Duration.Inf)
          Thread.sleep(20)
          Await.result(db.writeNative(SeedData.traceroute.subscriptionLine), Duration.Inf)
          println("Data sent.")
          andThen()
        }
    }

    def sendDataAnd(
      afterSend: () => Any = () => Unit,
      withSend : () => Any = () => Unit
    ): Unit = {
      sendData(andThen = afterSend)
      withSend()
    }

    "receive valid data" in {
      // TODO: Need a real address here
      val influxConfig = getInfluxConfig("mockMeasurementSourceContext", "130.217.250.59")

      def setEmpty(boolean: Boolean): Unit = MeasurementSubscriptionSourceFunctionTest.noDataReceived = boolean

      val func = new MeasurementSubscriptionSourceFunction
      func.overrideConfig(influxConfig)

      val ctx = new MockSourceContext[Measurement] {
        override var process: Seq[Measurement] => Unit = { elements =>
          setEmpty(elements.isEmpty)
        }
      }

      sendDataAnd(afterSend = { () =>
        func.cancel()
        ctx.close()
      }, withSend = () => func.run(ctx))

      assert(!MeasurementSubscriptionSourceFunctionTest.noDataReceived)
    }
  }
}

private object MeasurementSubscriptionSourceFunctionTest {
  var noDataReceived = false
}
