package nz.net.wand.amp.analyser

import com.typesafe.config.{Config, ConfigFactory}

trait Configuration {
  @transient final private[this] lazy val staticPrefix: String = getClass.getPackage.getName

  @transient protected[this] var config: Config = ConfigFactory.load(staticPrefix)

  private[this] var _configPrefix = s"$staticPrefix"
  protected[this] def configPrefix: String = _configPrefix
  protected[this] def configPrefix_=(prefix: String): Unit = {
    if (prefix.isEmpty) {
      config = ConfigFactory.load(staticPrefix)
      _configPrefix = staticPrefix
    }
    else {
      _configPrefix = s"$staticPrefix.$prefix"
      val topConfig = ConfigFactory.load()

      if (topConfig.hasPath(_configPrefix)) {
        config = topConfig.getConfig(_configPrefix)
      }
      else {
        config = ConfigFactory.empty()
      }
    }
  }

  protected[this] def getConfigInt(name: String): Option[Int] = {
    if (config.hasPath(name)) {
      val s = config.getString(name)
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
    if (config.hasPath(name)) {
      val s = config.getString(name)
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
