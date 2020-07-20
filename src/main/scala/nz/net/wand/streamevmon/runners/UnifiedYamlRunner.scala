package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.{Configuration, Logging}
import nz.net.wand.streamevmon.detectors.baseline.BaselineDetector
import nz.net.wand.streamevmon.detectors.HasFlinkConfig
import nz.net.wand.streamevmon.detectors.changepoint.{ChangepointDetector, NormalDistribution}
import nz.net.wand.streamevmon.detectors.distdiff.DistDiffDetector
import nz.net.wand.streamevmon.detectors.loss.LossDetector
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.detectors.spike.SpikeDetector
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.{AmpMeasurementSourceFunction, BigDataSourceFunction, InfluxSinkFunction}
import nz.net.wand.streamevmon.measurements.{CsvOutputable, HasDefault, Measurement}
import nz.net.wand.streamevmon.measurements.amp._
import nz.net.wand.streamevmon.measurements.bigdata.Flow

import java.time.Duration

import org.apache.commons.lang3.text.WordUtils
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.functions.sink.{PrintSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.scala._

import scala.collection.mutable
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
    lazy val isDefined: Boolean = value.isDefined
  }

  implicit def perhaps[E](implicit ev: E = null): Perhaps[E] = Perhaps(Option(ev))

  val detectorsBySink: mutable.Map[String, mutable.Buffer[DataStream[Event]]] = mutable.Map()

  type KeyedDetectorT = KeyedProcessFunction[String, Measurement, Event] with HasFlinkConfig

  def newDetectorFromName[MeasT <: Measurement : ClassTag](
    str: String
  )(
    implicit hasDefault: Perhaps[MeasT <:< HasDefault],
    csvOutputable      : Perhaps[MeasT <:< CsvOutputable]
  ): Option[KeyedDetectorT] = {

    object Requirement extends Enumeration {
      val HasDefault: Requirement.Value = Value("HasDefault")
      val CsvOutputable: Requirement.Value = Value("CsvOutputable")
    }

    val detectorRequirements = Map(
      "baseline" ->
        (
          Seq(Requirement.HasDefault),
          () => new BaselineDetector[MeasT with HasDefault]
        ),
      "changepoint" ->
        (
          Seq(Requirement.HasDefault),
          () => {
            implicit val normalDistributionTypeInformation: TypeInformation[NormalDistribution[MeasT with HasDefault]] =
              TypeInformation.of(classOf[NormalDistribution[MeasT with HasDefault]])
            new ChangepointDetector[MeasT with HasDefault, NormalDistribution[MeasT with HasDefault]](
              new NormalDistribution(mean = 0)
            )
          }
        ),
      "distdiff" ->
        (
          Seq(Requirement.HasDefault),
          () => new DistDiffDetector[MeasT with HasDefault]
        ),
      "mode" ->
        (
          Seq(Requirement.HasDefault),
          () => new ModeDetector[MeasT with HasDefault]
        ),
      "spike" ->
        (
          Seq(Requirement.HasDefault),
          () => new SpikeDetector[MeasT with HasDefault]
        ),
      "loss" ->
        (
          Seq(),
          () => new LossDetector[MeasT]
        ),
    )

    val detector = detectorRequirements.get(str) match {
      case Some(value) =>
        if (value._1.forall {
          case Requirement.HasDefault => hasDefault.isDefined
          case Requirement.CsvOutputable => csvOutputable.isDefined
        }) {
          Some(value._2())
        }
        else {
          logger.error(s"Could not create $str detector as ${classTag[MeasT].toString()} does not have ${value._1.mkString(",")}")
          None
        }
      case None =>
        logger.warn(s"Unknown detector type $str")
        None
    }
    detector.asInstanceOf[Option[KeyedDetectorT]]
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
        .flatMap { case (sinkName, enabled) =>
          detectorsBySink(sinkName) = mutable.Buffer()
          sinkName match {
            case "influx" => Some((sinkName, new InfluxSinkFunction))
            case "print" => Some((sinkName, new PrintSinkFunction[Event]()))
            case _ => logger.warn(s"Unknown sink name $sinkName"); None
          }
        }

    def recordSinkRequests(
      sinks: Map[String, Boolean],
      stream: DataStream[Event]
    ): Unit = {
      sinks.foreach { case (sinkName, enabled) =>
        if (enabled) {
          detectorsBySink(sinkName).append(stream)
        }
      }
    }

    def processDatatypeChildLayer[MeasT <: Measurement : ClassTag](
      dTypeChildren : Map[String, Map[String, Any]],
      source        : DataStream[Measurement],
      filteredStream: DataStream[Measurement]
    )(
      implicit pf: Perhaps[MeasT <:< HasDefault],
      pc         : Perhaps[MeasT <:< CsvOutputable]
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
          case detectorName =>
            newDetectorFromName[MeasT](detectorName).map { det =>
              val eventStream = filteredStream
                .keyAndTimeWindowStreams
                .addDetector(det)

              eventStream.map { stream =>
                recordSinkRequests(
                  grandchild.asInstanceOf[Map[String, Boolean]],
                  stream
                )
              }
            }
        }
      }
    }

    def processDatatypeLayer(
      dataTypes: Map[String, Map[String, Any]],
      source   : DataStream[Measurement]
    ): Unit = {
      dataTypes.foreach { case (dataType, children) =>
        dataType.toLowerCase match {
          // Amp
          case "dns" =>
            processDatatypeChildLayer[DNS](
              children.asMapParent,
              source,
              source.filterType[DNS]
            )
          case "http" =>
            processDatatypeChildLayer[HTTP](
              children.asMapParent,
              source,
              source.filterType[HTTP]
            )
          case "icmp" =>
            processDatatypeChildLayer[ICMP](
              children.asMapParent,
              source,
              source.filterType[ICMP]
            )
          case "tcpping" =>
            processDatatypeChildLayer[TCPPing](
              children.asMapParent,
              source,
              source.filterType[TCPPing]
            )
          case "traceroute" =>
            processDatatypeChildLayer[Traceroute](
              children.asMapParent,
              source,
              source.filterType[Traceroute]
            )
          // Bigdata
          case "flow" =>
            processDatatypeChildLayer[Flow](
              children.asMapParent,
              source,
              source.filterType[Flow]
            )

          case _ => logger.warn(s"Unknown datatype $dataType")
        }
      }
    }

    def processInfluxSourceLayer(sources: Map[String, Map[String, Any]]): Unit = {
      sources.foreach { case (sourceName, dTypes) =>
        sourceName.toLowerCase match {
          case "amp" =>
            processDatatypeLayer(
              dTypes.asMapParent,
              env
                .addSource(new AmpMeasurementSourceFunction)
                .name("AMP Measurement Subscription")
                .uid("amp-measurement-source")
                .asInstanceOf[DataStream[Measurement]]
            )

          case "bigdata" =>
            processDatatypeLayer(
              dTypes.asMapParent,
              env
                .addSource(new BigDataSourceFunction)
                .name("Libtrace-Bigdata Measurement Subscription")
                .uid("bigdata-measurement-source")
                .asInstanceOf[DataStream[Measurement]]
            )

          case _ => logger.warn(s"Unknown influx source name $sourceName")
        }
      }
    }

    def processEsmondSourceLayer(sources: Map[String, Map[String, Any]]): Unit = {
      // TODO
    }

    def processSourceLayer(sources: Map[String, Map[String, Any]]): Unit = {
      sources.foreach { case (sourceName, sourceTypes) =>
        sourceName.toLowerCase match {
          case "influx" => processInfluxSourceLayer(sourceTypes.asMapParent)
          case "esmond" => processEsmondSourceLayer(sourceTypes.asMapParent)
          case _ => logger.warn(s"Unknown source name $sourceName")
        }
      }
    }

    processSourceLayer(flows.asMapParent.getOrElse("flows", Map()).asMapParent)

    detectorsBySink.foreach {
      case (sinkName, dets) =>
        val union = dets.size match {
          case 0 => None
          case 1 => Some(dets.head)
          case _ => Some(dets.head.union(dets.drop(1): _*))
        }

        union.map { dets =>
          dets
            .addSink(sinks(sinkName))
            .name(WordUtils.capitalize(s"$sinkName Sink"))
            .uid(s"${sinkName.toLowerCase}-sink")
        }
    }

    env.execute()

    val breakpoint = 1
  }
}
