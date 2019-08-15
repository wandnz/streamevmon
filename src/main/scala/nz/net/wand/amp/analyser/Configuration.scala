package nz.net.wand.amp.analyser

import com.typesafe.config.{Config, ConfigFactory}

/** Allows accessing centralised configuration of the program's behaviour.
  *
  * Currently, an `application.properties` file is located automatically.
  *
  * All config keys begin with the static prefix "nz.net.wand.amp.analyser".
  *
  * A class using this trait can set a custom `configPrefix`. This is appended
  * to the static prefix, and followed by the key name supplied as an argument
  * to any call of a `getConfig` function when looking up a config key.
  *
  * @example
  * {{{
  *   configPrefix = "example"
  *   val result = getConfigString("element")
  * }}}
  *
  * This will return the value in key "nz.net.wand.amp.analyser.example.element"
  */
trait Configuration {
  @transient final private[this] lazy val staticPrefix: String = "nz.net.wand.amp.analyser"

  @transient private[this] var config: Config = ConfigFactory.load(staticPrefix)

  private[this] var _configPrefix = s"$staticPrefix"

  /** Gets the fully qualified config prefix.
    *
    * @return The fully qualified prefix, including the value of the static prefix.
    */
  protected[this] def configPrefix: String = _configPrefix

  /** Sets the custom config prefix.
    *
    * @param prefix The desired custom prefix, which is appended to the static prefix.
    */
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

  /** Get an integer-type configuration option.
    *
    * @param name The key containing a desired integer-type configuration option.
    *             This value is appended to the static prefix and the custom prefix.
    *
    * @return The option value, if present, or `None`
    */
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

  /** Get a string-type configuration option.
    *
    * @param name The key containing a desired string-type configuration option.
    *             This value is appended to the static prefix and the custom prefix.
    *
    * @return The option value, if present, or `None`
    */
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
