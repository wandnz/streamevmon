package nz.net.wand.streamevmon

import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

trait TestBase
  extends AnyWordSpec
          with Matchers
          with BeforeAndAfter {}
