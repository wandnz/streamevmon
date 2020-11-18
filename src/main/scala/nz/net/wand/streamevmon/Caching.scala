package nz.net.wand.streamevmon

import nz.net.wand.streamevmon.Caching.CacheMode

import java.time.Instant

import net.spy.memcached.compat.log.SLF4JLogger
import org.apache.flink.api.java.utils.ParameterTool
import scalacache.{sync, Cache}
import scalacache.caffeine._
import scalacache.memcached._
import scalacache.modes.sync._
import scalacache.serialization.binary._

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

/** Companion object storing the common in-memory cache.
  */
object Caching {

  protected object CacheMode extends Enumeration {
    type CacheMode = Value
    val InMemory, Memcached = Value
  }

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
  * ==Configuration==
  *
  * General caching configuration is configured by the `caching` config key
  * group.
  *
  * - `ttl`: While this class does not use this key directly, inheriting classes
  * may use this value as a user-configurable default expiry time, in seconds,
  * for cached values.
  * Default 30.
  *
  * The `caching.memcached` key group configured the memcached settings.
  * Caffeine caches are not affected by this group.
  *
  * - `enabled`: This class also does not directly use this key, but inheriting
  * classes which would like to support memcached caching should check the key's
  * boolean value before setting this class to memcached mode.
  * Default false.
  *
  * - `serverName`: The address which the memcached server is running at.
  * Default localhost.
  *
  * - `port`: The port which the memcached server is listening on.
  * Default 11211.
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

  implicit private def cache: Cache[Option[Any]] = cacheMode match {
    case CacheMode.InMemory => Caching.caffeineCache
    case CacheMode.Memcached => memcachedCache
  }

  private val internalUsedCacheKeys: mutable.Map[String, (Instant, Option[FiniteDuration])] = mutable.Map()

  protected def usedCacheKeys: mutable.Map[String, (Instant, Option[FiniteDuration])] = {
    internalUsedCacheKeys
      .filter(_._2._2.isDefined)
      .filter { case (_, value) =>
        value._1.plusNanos(value._2.get.toNanos).isBefore(Instant.now)
      }
      .foreach { case (key, _) =>
        internalUsedCacheKeys.remove(key)
      }
    internalUsedCacheKeys
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
  protected def getWithCache[T](
    key   : String,
    ttl   : Option[FiniteDuration],
    method: => Option[Any]
  ): Option[T] = {
    usedCacheKeys += key -> (Instant.now, ttl)
    sync.caching(key)(ttl)(method).asInstanceOf[Option[T]]
  }

  protected def invalidate(key: String): Unit = {
    sync.remove(key)
    usedCacheKeys.remove(key)
  }

  protected def invalidateAll(): Unit = {
    internalUsedCacheKeys.foreach { case (key, _) =>
      sync.remove(key)
      internalUsedCacheKeys.remove(key)
    }
  }
}
