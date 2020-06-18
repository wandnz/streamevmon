package nz.net.wand.streamevmon.measurements

import nz.net.wand.streamevmon.{SeedData, TestBase}
import nz.net.wand.streamevmon.measurements.esmond.{EsmondMeasurement, RichEsmondMeasurement}

class EsmondCreateTest extends TestBase {
  "Esmond Measurements" should {
    "be created correctly" in {
      SeedData.esmond.expectedObjects.foreach { set =>
        EsmondMeasurement(set.eventType, set.tsEntry) shouldEqual set.baseMeasurement
        RichEsmondMeasurement(set.eventType, set.tsEntry) shouldEqual set.baseRichMeasurement
        RichEsmondMeasurement(set.summary, set.tsEntry) shouldEqual set.summaryRichMeasurement
      }
    }
  }
}
