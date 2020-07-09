package nz.net.wand.streamevmon.connectors

import nz.net.wand.streamevmon.TestBase
import nz.net.wand.streamevmon.connectors.esmond.EsmondAPI

import java.time._

class EsmondTileToInstantsTest extends TestBase {
  "EsmondAPI.tileToTimeRange" should {
    "convert tile IDs to Instant tuples" in {
      // Checks for a few reasonable values since the epoch
      // Netbeam only actually has data since around 2015, so most of these
      // will never happen.
      EsmondAPI.tileToTimeRange(10957) shouldEqual
        (
          LocalDate.ofYearDay(2000, 1).atStartOfDay(ZoneOffset.UTC).toInstant,
          LocalDate.ofYearDay(2000, 2).atStartOfDay(ZoneOffset.UTC).minus(Duration.ofNanos(1)).toInstant
        )

      EsmondAPI.tileToTimeRange(0) shouldEqual
        (
          LocalDate.ofYearDay(1970, 1).atStartOfDay(ZoneOffset.UTC).toInstant,
          LocalDate.ofYearDay(1970, 2).atStartOfDay(ZoneOffset.UTC).minus(Duration.ofNanos(1)).toInstant
        )

      EsmondAPI.tileToTimeRange(18443) shouldEqual
        (
          LocalDate.ofYearDay(2020, 182).atStartOfDay(ZoneOffset.UTC).toInstant,
          LocalDate.ofYearDay(2020, 183).atStartOfDay(ZoneOffset.UTC).minus(Duration.ofNanos(1)).toInstant
        )

      // These timestamps were grabbed from Netbeam (the source of tiles). The start of the range is inclusive...
      EsmondAPI.tileToTimeRange(18443)._1 shouldEqual Instant.ofEpochMilli(1593475200000L)
      // and the end of the range returned by our function is just before the start of the next range.
      // The last value that you'll actually get from Netbeam will be before the end of the range.
      Duration.between(Instant.ofEpochMilli(1593561570000L), EsmondAPI.tileToTimeRange(18443)._2) should be < Duration.ofSeconds(31)
    }

    "parse strings correctly" in {
      EsmondAPI.tileToTimeRange("1d-10957") shouldEqual EsmondAPI.tileToTimeRange(10957)
      an[IllegalArgumentException] should be thrownBy EsmondAPI.tileToTimeRange("2d-5478")
    }
  }
}
