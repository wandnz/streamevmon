package nz.net.wand.streamevmon.runners.unified.schema

import nz.net.wand.streamevmon.detectors.baseline.BaselineDetector
import nz.net.wand.streamevmon.detectors.changepoint.{ChangepointDetector, NormalDistribution}
import nz.net.wand.streamevmon.detectors.distdiff.DistDiffDetector
import nz.net.wand.streamevmon.detectors.loss.LossDetector
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.detectors.spike.SpikeDetector
import nz.net.wand.streamevmon.detectors.HasFlinkConfig
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.measurements.{CsvOutputable, HasDefault, Measurement}
import nz.net.wand.streamevmon.Perhaps

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._

import scala.reflect._

/** This enum includes logic to build detectors. */
object DetectorType extends Enumeration {

  val Baseline: ValueBuilder = new ValueBuilder("baseline")
  val Changepoint: ValueBuilder = new ValueBuilder("changepoint")
  val DistDiff: ValueBuilder = new ValueBuilder("distdiff")
  val Loss: ValueBuilder = new ValueBuilder("loss")
  val Mode: ValueBuilder = new ValueBuilder("mode")
  val Spike: ValueBuilder = new ValueBuilder("spike")

  class ValueBuilder(name: String) extends Val(name) {

    /** Builds a detector with the specified Measurement type, or throws an
      * IllegalArgumentException if the type does not have the traits that this
      * detector type requires.
      *
      * @param hasDefault    Defined if `MeasT <: HasDefault`.
      * @param csvOutputable Defined if `MeasT <: CsvOutputable`.
      */
    def build[MeasT <: Measurement : ClassTag](
      implicit hasDefault: Perhaps[MeasT <:< HasDefault],
      csvOutputable      : Perhaps[MeasT <:< CsvOutputable]
    ): KeyedProcessFunction[String, Measurement, Event] with HasFlinkConfig = {
      lazy val noHasDefaultException = new IllegalArgumentException(s"Could not create $this detector as ${classTag[MeasT].toString()} does not have HasDefault!")
      lazy val noCsvOutputableException = new IllegalArgumentException(s"Could not create $this detector as ${classTag[MeasT].toString()} does not have CsvOutputable!")

      // Most of these detectors just require HasDefault. It would be lovely to
      // have a wrapper function to provide the check and exception, but I can't
      // get the type inference to work out nicely.
      val detector = this match {
        case Baseline =>
          if (hasDefault.isDefined) {
            new BaselineDetector[MeasT with HasDefault]
          }
          else {
            throw noHasDefaultException
          }
        case Changepoint =>
          if (hasDefault.isDefined) {
            // We need some extra TypeInformation here that can't be obtained
            // without an explicit implicit definition.
            implicit val normalDistributionTypeInformation: TypeInformation[NormalDistribution[MeasT with HasDefault]] =
            TypeInformation.of(classOf[NormalDistribution[MeasT with HasDefault]])
            new ChangepointDetector[MeasT with HasDefault, NormalDistribution[MeasT with HasDefault]](
              new NormalDistribution(mean = 0)
            )
          }
          else {
            throw noHasDefaultException
          }
        case DistDiff =>
          if (hasDefault.isDefined) {
            new DistDiffDetector[MeasT with HasDefault]
          }
          else {
            throw noHasDefaultException
          }
        // Loss detector doesn't care about any attributes, since it only uses isLossy.
        case Loss => new LossDetector[MeasT]
        case Mode =>
          if (hasDefault.isDefined) {
            new ModeDetector[MeasT with HasDefault]
          }
          else {
            throw noHasDefaultException
          }
        case Spike =>
          if (hasDefault.isDefined) {
            new SpikeDetector[MeasT with HasDefault]
          }
          else {
            throw noHasDefaultException
          }
      }
      detector.asInstanceOf[KeyedProcessFunction[String, Measurement, Event] with HasFlinkConfig]
    }
  }

}
