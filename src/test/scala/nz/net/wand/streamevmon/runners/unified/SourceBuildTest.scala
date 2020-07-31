package nz.net.wand.streamevmon.runners.unified

import nz.net.wand.streamevmon.TestBase
import nz.net.wand.streamevmon.connectors.esmond.{EsmondConnectionForeground, EsmondStreamDiscovery}
import nz.net.wand.streamevmon.flink._
import nz.net.wand.streamevmon.runners.unified.schema.{SourceInstance, SourceSubtype, SourceType}

import org.apache.flink.api.java.utils.ParameterTool

import scala.collection.JavaConverters._

class SourceBuildTest extends TestBase {
  "Sources should build correctly" when {
    "built by a SourceInstance" when {
      "source type is Influx/Amp" in {
        val srcInstance = SourceInstance(
          SourceType.Influx,
          Some(SourceSubtype.Amp),
          config = Map("extraKey" -> "true")
        )
        val built = srcInstance.buildSourceFunction

        built shouldBe an[AmpMeasurementSourceFunction]
        an[UnsupportedOperationException] should be thrownBy srcInstance.buildFileInputFormat
        built.configWithOverride(ParameterTool.fromArgs(Array())).toMap.asScala should contain(s"source.${built.configKeyGroup}.amp.extraKey" -> "true")
      }

      "source type is Influx/Bigdata" in {
        val srcInstance = SourceInstance(
          SourceType.Influx,
          Some(SourceSubtype.Bigdata),
          config = Map("extraKey" -> "true")
        )
        val built = srcInstance.buildSourceFunction

        built shouldBe a[BigDataSourceFunction]
        an[UnsupportedOperationException] should be thrownBy srcInstance.buildFileInputFormat
        built.configWithOverride(ParameterTool.fromArgs(Array())).toMap.asScala should contain(s"source.${built.configKeyGroup}.bigdata.extraKey" -> "true")
      }

      "source type is Esmond" in {
        val srcInstance = SourceInstance(
          SourceType.Esmond,
          None,
          config = Map("extraKey" -> "true")
        )
        val built = srcInstance.buildSourceFunction

        built shouldBe a[PollingEsmondSourceFunction[
          EsmondConnectionForeground,
          EsmondStreamDiscovery[EsmondConnectionForeground]
        ]]
        an[UnsupportedOperationException] should be thrownBy srcInstance.buildFileInputFormat
        built.configWithOverride(ParameterTool.fromArgs(Array())).toMap.asScala should contain(s"source.${built.configKeyGroup}.extraKey" -> "true")
      }

      "source type is LatencyTS/Amp" in {
        val srcInstance = SourceInstance(
          SourceType.LatencyTS,
          Some(SourceSubtype.LatencyTSAmp),
          config = Map("extraKey" -> "true")
        )
        val built = srcInstance.buildFileInputFormat

        built shouldBe a[LatencyTSAmpFileInputFormat]
        an[UnsupportedOperationException] should be thrownBy srcInstance.buildSourceFunction
        built.configWithOverride(ParameterTool.fromArgs(Array())).toMap.asScala should contain(s"source.${built.configKeyGroup}.ampicmp.extraKey" -> "true")
      }

      "source type is LatencyTS/Smokeping" in {
        val srcInstance = SourceInstance(
          SourceType.LatencyTS,
          Some(SourceSubtype.LatencyTSSmokeping),
          config = Map("extraKey" -> "true")
        )
        val built = srcInstance.buildFileInputFormat

        built shouldBe a[LatencyTSSmokepingFileInputFormat]
        an[UnsupportedOperationException] should be thrownBy srcInstance.buildSourceFunction
        built.configWithOverride(ParameterTool.fromArgs(Array())).toMap.asScala should contain(s"source.${built.configKeyGroup}.smokeping.extraKey" -> "true")
      }
    }
  }

  "Sources should fail to build" when {
    "built by a SourceInstance" when {
      "types are mismatched" in {
        val allInvalidCombinations = Seq(
          (SourceType.Influx, SourceSubtype.LatencyTSAmp),
          (SourceType.Influx, SourceSubtype.LatencyTSSmokeping),
          (SourceType.LatencyTS, SourceSubtype.Amp),
          (SourceType.LatencyTS, SourceSubtype.Bigdata),
        )

        allInvalidCombinations.foreach { case (t, st) =>
          withClue(s"When type is $t and subtype is $st,") {
            t match {
              case SourceType.Influx | SourceType.Esmond =>
                an[IllegalArgumentException] should be thrownBy SourceInstance(t, Some(st)).buildSourceFunction
              case SourceType.LatencyTS =>
                an[IllegalArgumentException] should be thrownBy SourceInstance(t, Some(st)).buildFileInputFormat
            }
          }
        }
      }
    }
  }
}
