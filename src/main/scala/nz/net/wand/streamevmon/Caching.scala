/* This file is part of streamevmon.
 *
 * Copyright (C) 2021  The University of Waikato, Hamilton, New Zealand
 *
 * Author: Daniel Oosterwijk
 *
 * All rights reserved.
 *
 * This code has been developed by the University of Waikato WAND
 * research group. For further information please see https://wand.nz,
 * or our Github organisation at https://github.com/wanduow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nz.net.wand.streamevmon

import nz.net.wand.streamevmon.Caching.CacheMode

import net.spy.memcached.compat.log.SLF4JLogger
import org.apache.flink.api.java.utils.ParameterTool
import scalacache.{sync, Cache}
import scalacache.caffeine._
import scalacache.memcached._
import scalacache.modes.sync._
import scalacache.serialization.Codec

import scala.concurrent.duration.FiniteDuration

/** Companion object storing the common in-memory cache.
  */
object Caching {
  protected object CacheMode extends Enumeration {
    type CacheMode = Value
    val InMemory, Memcached = Value
  }

  @transient final private lazy val caffeineCache: Cache[Any] = CaffeineCache[Any]
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
  * The `caching.memcached` key group configures the memcached settings.
  * Caffeine caches are not affected by this group. Note that Kryo serialization
  * is used alongside Memcached.
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

  @transient private var memcachedCache: Cache[Any] = _

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
    implicit val codec: Codec[Any] = new GZippingKryoCodec()
    memcachedCache = MemcachedCache(
      s"${p.get("caching.memcached.serverName")}:${p.getInt("caching.memcached.port")}"
    )
    cacheMode = CacheMode.Memcached
  }

  /** Begins using an in-memory Caffeine cache for all new caching operations. */
  protected def useInMemoryCache(): Unit = {
    cacheMode = CacheMode.InMemory
  }

  /** Gets the appropriate cache object for the current caching mode. */
  implicit private def cache: Cache[Any] = cacheMode match {
    case CacheMode.InMemory => Caching.caffeineCache
    case CacheMode.Memcached => memcachedCache
  }

  implicit private def cacheT[T]: Cache[T] = cache.asInstanceOf[Cache[T]]

  /** Adds caching to a given method, according to the previously set up
    * configuration.
    *
    * @param key    The cache key. This should be unique for a particular method
    *               result. This function provides no key uniqueness guarantee.
    * @param ttl    The Time-To-Live value of the particular cache item. If
    *               None, the item will never expire on its own.
    * @param method The method to obtain a cacheable result from.
    * @tparam T The return type of `method`.
    *
    * @return The result of `method`, regardless of whether it was
    *         obtained from the cache or from a new execution of `method`.
    */
  protected def getWithCache[T](
    key   : String,
    ttl   : Option[FiniteDuration],
    method: => T
  ): T = {
    sync.caching(key)(ttl)(method)
  }

  /** Removes an item from the cache. */
  protected def invalidate(key: String): Unit = {
    sync.remove(key)
  }

  /** Removes all items from the cache. */
  protected def invalidateAll(): Unit = {
    cache.removeAll()
  }
}
