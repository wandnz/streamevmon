package nz.net.wand.streamevmon.detectors.spike

import nz.net.wand.streamevmon.TestBase

/** This is the data from the original stack overflow question, linked in
  * [[SmoothedZScore]]'s description. The test simply ensures it works the
  * same as the original scala implementation.
  */
class SmoothedZScoreTest extends TestBase {
  "SmoothedZScore" should {
    "generate correct signals" in {
      val inputs = List[Double](
        1, 1, 1.1, 1, 0.9, 1, 1, 1.1, 1, 0.9, 1, 1.1, 1, 1, 0.9, 1, 1, 1.1, 1,
        1, 1, 1, 1.1, 0.9, 1, 1.1, 1, 1, 0.9, 1, 1.1, 1, 1, 1.1, 1, 0.8, 0.9, 1,
        1.2, 0.9, 1, 1, 1.1, 1.2, 1, 1.5, 1, 3, 2, 5, 3, 2, 1, 1, 1, 0.9, 1, 1,
        3, 2.6, 4, 3, 3.2, 2, 1, 1, 0.8, 4, 4, 2, 2.5, 1, 1, 1
      )
      val expectedOutput = List[Int](
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1,
        1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0,
        0, 0
      )

      val lag = 30
      val threshold = 5d
      val influence = 0d

      val tested = SmoothedZScore(lag, threshold, influence)

      val results = inputs.map(tested.addValue)

      results.map(_.id) shouldEqual expectedOutput
    }
  }
}
