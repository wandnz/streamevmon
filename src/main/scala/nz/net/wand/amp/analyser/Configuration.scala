package nz.net.wand.amp.analyser

import com.typesafe.config.ConfigFactory

trait Configuration {
  protected[this] val config: com.typesafe.config.Config = ConfigFactory.load()

  private[this] val staticPrefix: String = getClass.getPackage.toString

  private[this] var _configPrefix = ""

  protected[this] def configPrefix: String = _configPrefix

  protected[this] def configPrefix_=(prefix: String): Unit = {
    if (prefix.isEmpty) {
      _configPrefix = s"$staticPrefix."
    }
    else {
      _configPrefix = s"$staticPrefix.$prefix."
    }
  }

  protected[this] def getConfig[T](name: String): T = {
    config.getAnyRef(configPrefix + name).asInstanceOf[T]
  }
}
