package nz.net.wand.streamevmon.detectors.distdiff

import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.HasFlinkConfig
import nz.net.wand.streamevmon.measurements.{HasDefault, Measurement}

import java.time.Duration

import org.apache.flink.api.common.state.ValueStateDescriptor
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.windows.Window
import org.apache.flink.util.Collector

/** This detector measures the difference between the distributions of
  * two sets of measurements: those observed recently, and those observed
  * slightly less recently. If a significant change is noticed, an event
  * is emitted.
  *
  * This functions the same as the [[DistDiffDetector]], but is passed a
  * windowed keyed Flink stream rather than just a keyed one. This means that
  * Flink handles the life-cycle of old measurements and can better organise
  * late inputs.
  *
  * @tparam MeasT The type of measurement to analyse.
  */
class WindowedDistDiffDetector[MeasT <: Measurement with HasDefault, W <: Window]
  extends ProcessWindowFunction[MeasT, Event, String, W]
          with DistDiffLogic
          with HasFlinkConfig {

  final val flinkName = "Distribution Difference Detector (Windowed)"
  final val flinkUid = "windowed-distdiff-detector"
  final val configKeyGroup: String = "distdiff"

  /** Called during initialisation. Sets up persistent state variables and
    * configuration.
    */
  override def open(parameters: Configuration): Unit = {
    inEvent = getRuntimeContext.getState(
      new ValueStateDescriptor[Boolean](
        "Is an event happening?",
        createTypeInformation[Boolean]
      )
    )

    val config = configWithOverride(getRuntimeContext)
    val prefix = s"detector.$configKeyGroup"
    recentsCount = config.getInt(s"$prefix.recentsCount")
    zThreshold = config.getDouble(s"$prefix.zThreshold")
    dropExtremeN = config.getInt(s"$prefix.dropExtremeN")
    minimumChange = config.getDouble(s"$prefix.minimumChange")
  }

  /** Emits an event based on the provided severity and distributions. */
  private def newEvent(
    value: MeasT,
    elements: Seq[MeasT],
    old: Seq[Double],
    rec: Seq[Double],
    severity: Int,
    out: Collector[Event]
  ): Unit = {
    out.collect {
      val oldMean = old.sum / old.size
      val recMean = rec.sum / rec.size
      Event(
        "distdiff_events",
        value.stream,
        severity,
        value.time,
        Duration.between(elements.head.time, elements(recentsCount).time),
        s"Distribution of ${value.getClass.getSimpleName} has changed. " +
          s"Mean has ${
            if (oldMean < recMean) {
              "increased"
            }
            else {
              "decreased"
            }
          } from $oldMean to $recMean",
        Map(
          "windowed" -> "true"
        )
      )
    }
  }

  /** New measurements are ingested here. */
  override def process(
    key                   : String,
    context               : Context,
    elements              : Iterable[MeasT],
    out                   : Collector[Event]
  ): Unit = {
    // If we don't have enough elements yet, we can't do anything.
    if (elements.size < recentsCount * 2) {
      return
    }

    // The algorithm needs the lists to be sorted and the outliers pruned.
    val sorted = elements.map(identity).toSeq.sortBy(_.time)
    val sortedAndMapped = sorted.map(_.defaultValue.get)

    val old = sortedAndMapped.take(recentsCount).sorted.drop(dropExtremeN).dropRight(dropExtremeN)
    val rec = sortedAndMapped.drop(recentsCount).sorted.drop(dropExtremeN).dropRight(dropExtremeN)

    // Get the 'z-value'...
    val diff = distributionDifference(old, rec)
    // ... and pass it to the severity calculator.
    val severity = eventSeverity(old, rec, diff)
    // If the severity is None, it wasn't an event.
    if (severity.isDefined) {
      newEvent(elements.head, sorted, old, rec, severity.get, out)
      inEvent.update(true)
    }
    // If the difference between distributions gets low enough, then we're no
    // longer in an event.
    if (diff < zThreshold / 2) {
      inEvent.update(false)
    }
  }
}
