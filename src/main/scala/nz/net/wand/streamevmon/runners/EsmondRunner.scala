package nz.net.wand.streamevmon.runners

import nz.net.wand.streamevmon.connectors.esmond.Controller

object EsmondRunner {
  def main(args: Array[String]): Unit = {

    val controller = new Controller()
    controller.start()
  }
}
