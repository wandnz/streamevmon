package nz.net.wand.streamevmon.runners.unified

import nz.net.wand.streamevmon.TestBase
import nz.net.wand.streamevmon.detectors.baseline.BaselineDetector
import nz.net.wand.streamevmon.detectors.changepoint.{ChangepointDetector, Distribution}
import nz.net.wand.streamevmon.detectors.distdiff.{DistDiffDetector, WindowedDistDiffDetector}
import nz.net.wand.streamevmon.detectors.loss.LossDetector
import nz.net.wand.streamevmon.detectors.mode.ModeDetector
import nz.net.wand.streamevmon.detectors.spike.SpikeDetector
import nz.net.wand.streamevmon.events.Event
import nz.net.wand.streamevmon.flink.{HasFlinkConfig, WindowedFunctionWrapper}
import nz.net.wand.streamevmon.measurements.amp.ICMP
import nz.net.wand.streamevmon.runners.unified.schema._

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.windows.Window

import scala.collection.JavaConverters._
import scala.language.existentials
import scala.reflect.ClassTag

class DetectorBuildTest extends TestBase {

  "Detectors should build correctly" when {
    "built by a DetectorSchema" when {

      def getSchema(detType: DetectorType.ValueBuilder): DetectorSchema = {
        DetectorSchema(
          detType,
          Seq(
            DetectorInstance(
              Seq(
                // ICMP has HasDefault and CsvOutputable, so we'll use it for
                // cases where we need all instances to build
                SourceReference(
                  "imaginary-everything-source",
                  SourceDatatype.ICMP,
                  filterLossy = true
                )
              ),
              // Sink references don't matter for this test
              Seq(),
              config = Map("extraKey" -> "true")
            )
          )
        )
      }

      def checkForWrappedDetectors[
        DetT <: KeyedProcessFunction[String, ICMP, Event] with HasFlinkConfig : ClassTag
      ](
        detType: DetectorType.ValueBuilder
      ): Unit = {
        val schema = getSchema(detType)

        val built = schema.buildKeyed.head._2
        built shouldBe a[DetT]
        built.configWithOverride(ParameterTool.fromArgs(Array())).toMap.asScala should contain(s"detector.${built.configKeyGroup}.extraKey" -> "true")

        val builtWindowed = schema.buildWindowed.head._2
        builtWindowed shouldBe a[WindowedFunctionWrapper[_, _]]

        val typed = builtWindowed.asInstanceOf[WindowedFunctionWrapper[_, _]]
        typed.processFunction shouldBe a[DetT]

        typed.configWithOverride(ParameterTool.fromArgs(Array())).toMap.asScala should contain(s"detector.${typed.configKeyGroup}.extraKey" -> "true")
        typed.processFunction.configWithOverride(ParameterTool.fromArgs(Array())).toMap.asScala should contain(s"detector.${typed.processFunction.configKeyGroup}.extraKey" -> "true")
      }

      def checkForDetectorsImplementingWindowing[
        DetT <: KeyedProcessFunction[String, ICMP, Event] with HasFlinkConfig : ClassTag,
        WDetT <: ProcessWindowFunction[ICMP, Event, String, Window] with HasFlinkConfig : ClassTag
      ](
        detType: DetectorType.ValueBuilder
      ): Unit = {
        val schema = getSchema(detType)

        val built = schema.buildKeyed.head._2
        built shouldBe a[DetT]
        built.configWithOverride(ParameterTool.fromArgs(Array())).toMap.asScala should contain(s"detector.${built.configKeyGroup}.extraKey" -> "true")

        val builtWindowed = schema.buildWindowed.head._2
        builtWindowed shouldBe a[WDetT]
        builtWindowed.configWithOverride(ParameterTool.fromArgs(Array())).toMap.asScala should contain(s"detector.${builtWindowed.configKeyGroup}.extraKey" -> "true")
      }

      "detector type is baseline" in {
        checkForWrappedDetectors[BaselineDetector[ICMP]](DetectorType.Baseline)
      }

      "detector type is changepoint" in {
        checkForWrappedDetectors[ChangepointDetector[ICMP, Distribution[ICMP]]](DetectorType.Changepoint)
      }

      "detector type is distdiff" in {
        checkForDetectorsImplementingWindowing[DistDiffDetector[ICMP], WindowedDistDiffDetector[ICMP, Window]](DetectorType.DistDiff)
      }

      "detector type is loss" in {
        checkForWrappedDetectors[LossDetector[ICMP]](DetectorType.Loss)
      }

      "detector type is mode" in {
        checkForWrappedDetectors[ModeDetector[ICMP]](DetectorType.Mode)
      }

      "detector type is spike" in {
        checkForWrappedDetectors[SpikeDetector[ICMP]](DetectorType.Spike)
      }
    }
  }

  "Detectors should only build correctly" when {
    "datatype has correct traits" when {

      def checkByRequirements(
        detType: DetectorType.ValueBuilder,
        needsHasDefault: Boolean,
        needsCsvOutputable: Boolean
      ): Unit = {
        def getInstanceWithDatatype(t: SourceDatatype.Value): DetectorInstance = {
          DetectorInstance(
            Seq(
              SourceReference(
                "imaginary-everything-source",
                t,
                filterLossy = true
              )
            ),
            Seq(),
            config = Map("extraKey" -> "true")
          )
        }

        val withHasDefault = getInstanceWithDatatype(SourceDatatype.ICMP)
        val withoutHasDefault = getInstanceWithDatatype(SourceDatatype.Failure)
        val withCsvOutputable = getInstanceWithDatatype(SourceDatatype.LatencyTSAmp)
        val withoutCsvOutputable = getInstanceWithDatatype(SourceDatatype.Failure)

        noException should be thrownBy withHasDefault.buildKeyed(detType)
        noException should be thrownBy withHasDefault.buildWindowed(detType)
        if (needsHasDefault) {
          an[IllegalArgumentException] should be thrownBy withoutHasDefault.buildKeyed(detType)
          an[IllegalArgumentException] should be thrownBy withoutHasDefault.buildWindowed(detType)
        }

        noException should be thrownBy withCsvOutputable.buildKeyed(detType)
        noException should be thrownBy withCsvOutputable.buildWindowed(detType)
        if (needsCsvOutputable) {
          an[IllegalArgumentException] should be thrownBy withoutCsvOutputable.buildKeyed(detType)
          an[IllegalArgumentException] should be thrownBy withoutCsvOutputable.buildWindowed(detType)
        }
      }

      "detector type is baseline" in {
        checkByRequirements(
          DetectorType.Baseline,
          needsHasDefault = true,
          needsCsvOutputable = false
        )
      }

      "detector type is changepoint" in {
        checkByRequirements(
          DetectorType.Changepoint,
          needsHasDefault = true,
          needsCsvOutputable = false
        )
      }

      "detector type is distdiff" in {
        checkByRequirements(
          DetectorType.DistDiff,
          needsHasDefault = true,
          needsCsvOutputable = false
        )
      }

      "detector type is loss" in {
        checkByRequirements(
          DetectorType.Loss,
          needsHasDefault = false,
          needsCsvOutputable = false
        )
      }

      "detector type is mode" in {
        checkByRequirements(
          DetectorType.Mode,
          needsHasDefault = true,
          needsCsvOutputable = false
        )
      }

      "detector type is spike" in {
        checkByRequirements(
          DetectorType.Spike,
          needsHasDefault = true,
          needsCsvOutputable = false
        )
      }
    }
  }
}
