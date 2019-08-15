package nz.net.wand.amp.analyser

import scalacache.{sync, Cache}
import scalacache.caffeine._
import scalacache.modes.sync._

import scala.concurrent.duration._

/** Companion object storing common values for all caching functions.
  */
object Caching extends Configuration {
  @transient implicit final private lazy val cache: Cache[Option[Any]] = CaffeineCache[Option[Any]]

  configPrefix = "caching"

  @transient implicit final private lazy val ttl: Option[FiniteDuration] =
    getConfigInt("ttl").map(x => x.seconds)
}

/** Allows caching for the results of long-running functions.
  *
  * All functions that include caching have the same configuration and go into
  * the same cache.
  *
  * The `caching.ttl` configuration option sets the number of seconds a given
  * item will remain cached for.
  *
  * Currently, the cache is an in-memory Caffeine cache.
  *
  * @see [[https://github.com/ben-manes/caffeine]]
  * @example
  * {{{
  *   getWithCache(
  *     "my-fancy-key",
  *     {
  *       longRunningFunction()
  *     }
  *   )
  * }}}
  */
trait Caching {
  import Caching._

  /** Adds caching to a given method, according to the global configuration.
    *
    * @param key    The cache key. This should be unique for a particular method
    *               result. This function provides no key uniqueness guarantee.
    * @param method The method to obtain a cacheable result from.
    * @tparam T The return type of `method`.
    *
    * @return The result of `method`, regardless of whether it was
    *         obtained from the cache or from a new execution of `method`.
    */
  protected[this] def getWithCache[T](key: String, method: => Option[Any]): Option[T] = {
    sync.caching(key)(implicitly)(method).asInstanceOf[Option[T]]
  }
}
