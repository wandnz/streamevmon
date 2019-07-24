package nz.net.wand.amp.analyser

import com.typesafe.config.{Config, ConfigFactory}

trait Configuration {
  @transient final protected[this] lazy val config: Config = ConfigFactory.load()

  @transient final private[this] lazy val staticPrefix: String = getClass.getPackage.getName

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

  protected[this] def getConfigInt(name: String): Int = {
    config.getInt(configPrefix + name)
  }

  protected[this] def getConfigString(name: String): String = {
    config.getString(configPrefix + name)
  }
}
