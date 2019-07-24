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

  protected[this] def getConfigInt(name: String): Option[Int] = {
    if (config.hasPath(configPrefix + name)) {
      val s = config.getString(configPrefix + name)
      if (s.isEmpty) {
        None
      }
      else {
        Some(s.toInt)
      }
    }
    else {
      None
    }
  }

  protected[this] def getConfigString(name: String): Option[String] = {
    if (config.hasPath(configPrefix + name)) {
      val s = config.getString(configPrefix + name)
      if (s.isEmpty) {
        None
      }
      else {
        Some(s)
      }
    }
    else {
      None
    }
  }
}
