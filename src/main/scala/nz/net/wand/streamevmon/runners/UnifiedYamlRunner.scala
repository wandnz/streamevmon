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

import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.functions.sink.{PrintSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.scala._

import scala.collection.mutable
import scala.collection.JavaConverters._
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
    // == Setup flink config ==
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    config = Configuration.get(args)
    env.getConfig.setGlobalJobParameters(config)

    env.disableOperatorChaining

    env.enableCheckpointing(
      Duration.ofSeconds(config.getInt("flink.checkpointInterval")).toMillis,
      CheckpointingMode.EXACTLY_ONCE
    )

    env.setRestartStrategy(RestartStrategies.noRestart())

    // == Parse flow config key ==
    val flows = Configuration.getFlowsAsMap

    val sinks: Map[String, SinkFunction[Event] with HasFlinkConfig] =
      flows
        .asMapParent
        // This config key names the sinks that get used in the config.
        .getOrElse("defaultFlowSinks", Map())
        .flatMap { case (sinkName, enabled) =>
          // We'll give each of them a list of detectors, and instantiate them.
          detectorsBySink(sinkName) = mutable.Buffer()
          val sink = sinkName match {
            // This includes some setup for some of them.
            case "influx" => Some((sinkName, new InfluxSinkFunction))
            case "print" => Some((sinkName,
              new PrintSinkFunction[Event]() with HasFlinkConfig {
                override val flinkName: String = "Print: Std Out"
                override val flinkUid: String = "print-sink"
                override val configKeyGroup: String = ""
              }
            ))
            case _ => logger.warn(s"Unknown sink name $sinkName"); None
          }
          sink.asInstanceOf[Option[(String, SinkFunction[Event] with HasFlinkConfig)]]
        }

    /** The lowest-level key sets whether the detector wants to output to each
      * sink. If the value is true, add it.
      */
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
          // This layer can include a directive to filter out lossy measurements.
          // All measurements can be filtered by lossiness.
          case "filterlossy" =>
            val furtherFilteredStream = filteredStream.notLossy[MeasT]
            // The child of this layer is the same type of layer again.
            processDatatypeChildLayer(
              grandchild.asMapParent,
              source,
              furtherFilteredStream
            )
          // Any other directive is a detector name. We should create the
          // detector, then figure out where it wants its output to be sunk to.
          case detectorName =>
            newDetectorFromName[MeasT](detectorName).map { det =>
              // The user might specify extra config here, which we should
              // merge in and pass through.
              val overrideConfigMap = Configuration.flattenMap(
                Map(
                  s"detector.${det.configKeyGroup}" -> grandchild.getOrElse("config", Map())
                )
              )

              det.overrideConfig(ParameterTool.fromMap(overrideConfigMap.asJava))

              // Add the detector to its input
              val eventStream = filteredStream
                .keyAndTimeWindowStreams
                .addDetector(det)

              // And set it up to be added to its output(s)
              eventStream.map { stream =>
                recordSinkRequests(
                  grandchild.getOrElse("sinks", Map()).asInstanceOf[Map[String, Boolean]],
                  stream
                )
              }
            }
        }
      }
    }

    /** This layer just filters a stream by concrete type. Detectors should
      * specify which datatypes they accept in the config.
      */
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
        val source = sourceName.toLowerCase match {
          case "amp" => Some {
            val func = new AmpMeasurementSourceFunction
            func.overrideConfig(ParameterTool.fromMap(Map(
              "source.influx.amp.subscriptionName" -> "UnifiedRunnerSubscription"
            ).asJava))
            func
          }
          case "bigdata" => Some {
            val func = new BigDataSourceFunction
            func.overrideConfig(ParameterTool.fromMap(Map(
              "source.influx.bigdata.subscriptionName" -> "UnifiedRunnerSubscription"
            ).asJava))
            func
          }
          case _ => logger.warn(s"Unknown influx source name $sourceName"); None
        }
        source.map { s =>
          processDatatypeLayer(
            dTypes.asMapParent,
            env
              .addSource(s)
              .name(s.flinkName)
              .uid(s.flinkUid)
              .asInstanceOf[DataStream[Measurement]]
          )
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
            .name(sinks(sinkName).flinkName)
            .uid(sinks(sinkName).flinkUid)
        }
    }

    val breakpoint = 1

    env.execute()
  }
}
