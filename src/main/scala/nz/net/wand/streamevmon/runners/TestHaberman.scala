package nz.net.wand.streamevmon.runners

object TestHaberman {
  def main(args: Array[String]): Unit = {
    RnsapRunner.testHaberman()
  }
}

object TestUNSW8 {
  def main(args: Array[String]): Unit = {
    //RnsapRunner.testUnsw5()
    RnsapRunner.testUnsw8()
  }
}

object TestKDDAll {
  def main(args: Array[String]): Unit = {
    RnsapRunner.testKdd11()
  }
}

object TestPostBackfiltering {
  def main(args: Array[String]): Unit = {
    RnsapRunner.testPostBackfiltering()
  }
}
