package nz.net.wand.streamevmon

import net.spy.memcached.compat.log.SLF4JLogger
import org.apache.flink.api.java.utils.ParameterTool
import scalacache.{sync, Cache}
import scalacache.caffeine._
import scalacache.memcached._
import scalacache.modes.sync._
import scalacache.serialization.binary._

import scala.concurrent.duration.FiniteDuration

/** Companion object storing the common in-memory cache.
  */
object Caching {
  @transient final private lazy val caffeineCache: Cache[Option[Any]] = CaffeineCache[Option[Any]]
}

/** Allows caching for the results of long-running functions. Supports the
  * following cache implementations:
  *
  * - A program-wide in-memory Caffeine cache.
  *
  * - An external Memcached instance.
  *
  * Defaults to a Caffeine cache. Use the `useMemcached` and `useInMemoryCache`
  * methods to change caching mode. Be aware that changing mode will not
  * transfer any currently cached results.
  *
  * @see [[https://github.com/ben-manes/caffeine]]
  * @see [[https://memcached.org/]]
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
  protected var cacheMode: CacheMode.Value = CacheMode.InMemory

  protected object CacheMode extends Enumeration {
    type CacheMode = Value
    val InMemory, Memcached = Value
  }

  @transient private var memcachedCache: Cache[Option[Any]] = _

  // Ensure memcached uses the correct logging implementation.
  System.setProperty("net.spy.log.LoggerImpl", classOf[SLF4JLogger].getCanonicalName)

  /** Sets up a connection to a Memcached instance, and starts to use it for
    * all new caching operations.
    *
    * @param p The global parameters, which must contain the following keys:
    *          `caching.memcached.serverName`: Defaults to "localhost".
    *          `caching.memcached.port`: Defaults to 11211.
    */
  protected def useMemcached(p: ParameterTool): Unit = {
    memcachedCache = MemcachedCache(
      s"${p.get("caching.memcached.serverName")}:${p.getInt("caching.memcached.port")}")
    cacheMode = CacheMode.Memcached
  }

  /** Begins using an in-memory Caffeine cache for all new caching operations. */
  protected def useInMemoryCache(): Unit = {
    cacheMode = CacheMode.InMemory
  }

  implicit private[this] def cache: Cache[Option[Any]] = cacheMode match {
    case CacheMode.InMemory => Caching.caffeineCache
    case CacheMode.Memcached => memcachedCache
  }

  /** Adds caching to a given method, according to the previously set up
    * configuration.
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
  protected[this] def getWithCache[T](
    key: String,
    ttl: Option[FiniteDuration],
    method: => Option[Any]
  ): Option[T] = {
    sync.caching(key)(ttl)(method).asInstanceOf[Option[T]]
  }

  protected[this] def invalidate(key: String): Unit = {
    sync.remove(key)
  }
}
