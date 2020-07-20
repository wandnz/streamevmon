package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.{Configuration, Logging}
import nz.net.wand.streamevmon.detectors.baseline.BaselineDetector
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.{AmpMeasurementSourceFunction, InfluxSinkFunction}
import nz.net.wand.streamevmon.measurements.{CsvOutputable, HasDefault, Measurement}
import nz.net.wand.streamevmon.measurements.amp._

import java.time.Duration

import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.functions.sink.{PrintSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.scala._

import scala.reflect._

object UnifiedYamlRunner extends UnifiedRunnerExtensions with Logging {

  implicit class MapToMapWithMap[T](map: Map[String, T]) {
    implicit def asMapParent: Map[String, Map[String, T]] = {
      try {
        map.asInstanceOf[Map[String, Map[String, T]]]
      }
      catch {
        case e: Exception =>
          logger.error(e.toString)
          Map()
      }
    }
  }

  case class Perhaps[E](value: Option[E]) {
    def fold[F](ifAbsent: => F)(ifPresent: E => F): F = {
      value.fold(ifAbsent)(ifPresent)
    }
  }

  implicit def perhaps[E](implicit ev: E = null): Perhaps[E] = Perhaps(Option(ev))

  def newDetectorFromName[MeasT <: Measurement : ClassTag](
    str: String
  )(
    implicit pd: Perhaps[MeasT <:< HasDefault],
    pc: Perhaps[MeasT <:< CsvOutputable]
  ): Option[KeyedProcessFunction[String, MeasT, Event]] = {

    str match {
      case "baseline" => pd.fold[Option[KeyedProcessFunction[String, MeasT, Event]]] {
        logger.warn(s"Cannot create BaselineDetector: ${classTag[MeasT].toString()} does not have HasDefault")
        None
      } {
        _ =>
          Some(new BaselineDetector[MeasT with HasDefault])
            .asInstanceOf[Option[KeyedProcessFunction[String, MeasT, Event]]]
      }
      case _ => None
    }
  }

  def main(args: Array[String]): Unit = {
    // == Setup config ==
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    System.setProperty("source.influx.amp.subscriptionName", "AmpUnifiedRunner")
    System.setProperty("source.influx.bigdata.subscriptionName", "BigDataUnifiedRunner")

    config = Configuration.get(args)
    env.getConfig.setGlobalJobParameters(config)

    env.disableOperatorChaining

    env.enableCheckpointing(
      Duration.ofSeconds(config.getInt("flink.checkpointInterval")).toMillis,
      CheckpointingMode.EXACTLY_ONCE
    )

    // == Parse flow config key ==

    val flows = Configuration.getFlowsAsMap

    val sinks: Map[String, SinkFunction[Event]] =
      flows
        .asMapParent
        .getOrElse("defaultFlowSinks", Map())
        .map { case (sinkName, enabled) =>
          sinkName match {
            case "influx" => (sinkName, new InfluxSinkFunction)
            case "print" => (sinkName, new PrintSinkFunction[Event]())
          }
        }

    def processDatatypeChildLayer[MeasT <: Measurement : ClassTag](
      dTypeChildren : Map[String, Map[String, Any]],
      source        : DataStream[Measurement],
      filteredStream: DataStream[Measurement]
    )(
      implicit pf: Perhaps[MeasT <:< HasDefault],
      pc: Perhaps[MeasT <:< CsvOutputable]
    ): Unit = {
      dTypeChildren.foreach { case (child, grandchild) =>
        child.toLowerCase match {
          case "filterlossy" =>
            val furtherFilteredStream = filteredStream.notLossy[MeasT]
            processDatatypeChildLayer(
              grandchild.asMapParent,
              source,
              furtherFilteredStream
            )
          case detectorName => newDetectorFromName[MeasT](detectorName)
        }
      }
    }

    def processDatatypeLayer(
      dataTypes: Map[String, Map[String, Any]],
      source   : DataStream[Measurement]
    ): Unit = {
      dataTypes.foreach { case (dataType, children) =>
        dataType.toLowerCase match {
          case d@"icmp" =>
            processDatatypeChildLayer[ICMP](
              children.asMapParent,
              source,
              source.filterType[ICMP]
            )

          case d@"dns" =>
            processDatatypeChildLayer[DNS](
              children.asMapParent,
              source,
              source.filterType[DNS]
            )

          case _ => logger.warn(s"Unknown datatype $dataType")
        }
      }
    }

    def processInfluxSourceLayer(sources: Map[String, Map[String, Any]]): Unit = {
      sources.foreach { case (sourceName, dTypes) =>
        sourceName.toLowerCase match {
          case s@"amp" =>
            val source = env
              .addSource(new AmpMeasurementSourceFunction)
              .name("AMP Measurement Subscription")
              .uid("amp-measurement-source")
              .asInstanceOf[DataStream[Measurement]]

            processDatatypeLayer(dTypes.asMapParent, source)

          case _ => logger.warn(s"Unknown influx source name $sourceName")
        }
      }
    }

    def processSourceLayer(sources: Map[String, Map[String, Any]]): Unit = {
      sources.foreach { case (sourceName, sourceTypes) =>
        sourceName.toLowerCase match {
          case s@"influx" => processInfluxSourceLayer(sourceTypes.asMapParent)
          case _ => logger.warn(s"Unknown source name $sourceName")
        }
      }
    }

    processSourceLayer(flows.asMapParent.getOrElse("flows", Map()).asMapParent)

    val breakpoint = 1
  }
}
