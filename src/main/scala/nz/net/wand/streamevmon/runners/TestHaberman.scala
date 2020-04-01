package nz.net.wand.streamevmon.runners

object TestHaberman {
  def main(args: Array[String]): Unit = {
    RnsapRunner.testHaberman()
  }
}

object TestUNSW8_1 {
  def main(args: Array[String]): Unit = {
    RnsapRunner.testUnsw8PBF(Seq(1.0, 30.0))
  }
}

object TestUNSW8_2 {
  def main(args: Array[String]): Unit = {
    RnsapRunner.testUnsw8PBF(Seq(2.0, 20.0))
  }
}

object TestUNSW8_3 {
  def main(args: Array[String]): Unit = {
    RnsapRunner.testUnsw8PBF(Seq(10.0))
  }
}

object TestUNSW8_4 {
  def main(args: Array[String]): Unit = {
    RnsapRunner.testUnsw8PBF(Seq(5.0, 15.0))
  }
}

object TestUNSW5_1 {
  def main(args: Array[String]): Unit = {
    RnsapRunner.testUnsw5PBF(Seq(1.0, 30.0))
  }
}

object TestUNSW5_2 {
  def main(args: Array[String]): Unit = {
    RnsapRunner.testUnsw5PBF(Seq(2.0, 20.0))
  }
}

object TestUNSW5_3 {
  def main(args: Array[String]): Unit = {
    RnsapRunner.testUnsw5PBF(Seq(5.0, 15.0))
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
