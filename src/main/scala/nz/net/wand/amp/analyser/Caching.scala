package nz.net.wand.amp.analyser

import scalacache.{sync, Cache}
import scalacache.caffeine._
import scalacache.modes.sync._

import scala.concurrent.duration.FiniteDuration

/** Companion object storing the common cache.
  */
object Caching {
  @transient implicit final private lazy val cache: Cache[Option[Any]] = CaffeineCache[Option[Any]]
}

/** Allows caching for the results of long-running functions.
  *
  * A single cache is used program-wide.
  *
  * Currently, the cache is an in-memory Caffeine cache.
  *
  * @see [[https://github.com/ben-manes/caffeine]]
  * @example
  * {{{
  *   getWithCache(
  *     "my-fancy-key",
  *     None,
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
    * @param ttl    The Time-To-Live value of the particular cache item.
    * @param method The method to obtain a cacheable result from.
    * @tparam T The return type of `method`.
    *
    * @return The result of `method`, regardless of whether it was
    *         obtained from the cache or from a new execution of `method`.
    */
  protected[this] def getWithCache[T](key: String, ttl: Option[FiniteDuration], method: => Option[Any]): Option[T] = {
    sync.caching(key)(ttl)(method).asInstanceOf[Option[T]]
  }
}
