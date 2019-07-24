package nz.net.wand.amp.analyser

import scalacache.{sync, Cache}
import scalacache.caffeine._
import scalacache.modes.sync._

import scala.concurrent.duration._

object Caching extends Configuration {
  @transient implicit final private lazy val cache: Cache[Option[Any]] = CaffeineCache[Option[Any]]

  configPrefix = "caching"

  @transient implicit final private lazy val ttl: Option[FiniteDuration] =
    getConfigInt("ttl").map(x => x.seconds)
}

trait Caching {
  import Caching._

  protected[this] def getWithCache[T](key: String, method: => Option[Any]): Option[T] = {
    sync.caching(key)(implicitly)(method).asInstanceOf[Option[T]]
  }
}
