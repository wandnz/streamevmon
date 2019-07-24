package nz.net.wand.amp.analyser

import scalacache.caffeine._
import scalacache.modes.sync._
import scalacache.{Cache, sync}

import scala.concurrent.duration._

object Caching extends Configuration {
  implicit final val cache: Cache[Option[Any]] = CaffeineCache[Option[Any]]

  configPrefix = "caching"
  implicit final val ttl: Some[FiniteDuration] = Some(getConfigInt("ttl").seconds)
}

trait Caching {
  import Caching._

  def getWithCache[T](key: String, method: => Option[Any]): Option[T] = {
    sync.caching(key)(implicitly)(method).asInstanceOf[Option[T]]
  }
}
