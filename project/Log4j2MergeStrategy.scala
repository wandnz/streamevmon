/* This file is from https://github.com/Dwolla/sbt-assembly-log4j2/blob/master/src/main/scala/sbtassembly/Log4j2MergeStrategy.scala,
 * which is a fork of the project of the same name by idio. It falls under the
 * MIT license, reproduced below (but with more comments).
 *
 * It is usually packaged as an SBT module distributed on Bintray, but Bintray
 * ends service in 2021, so we reproduce the code here.
 *
 * Copyright 2017 idio Ltd
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

import java.io.{File, FileOutputStream}

import org.apache.logging.log4j.core.config.plugins.processor.PluginCache
import sbtassembly.MergeStrategy

import scala.collection.JavaConverters._

/** sbt-assembly merge strategy that utilises log4j2's PluginCache functionality
  * to merge multiple plugin cache files.
  *
  * @see [[https://github.com/Dwolla/sbt-assembly-log4j2]]
  */
object Log4j2MergeStrategy {
  val strategy: MergeStrategy = new MergeStrategy {
    val name = "log4j2::plugincache"

    def apply(
      tempDir: File,
      path   : String,
      files  : Seq[File]
    ): Either[String, Seq[(File, String)]] = {
      val file = MergeStrategy.createMergeTarget(tempDir, path)
      val out = new FileOutputStream(file)

      val aggregator = new PluginCache()
      val filesEnum = files.toIterator.map(_.toURI.toURL).asJavaEnumeration

      try {
        aggregator.loadCacheFiles(filesEnum)
        aggregator.writeCache(out)
        Right(Seq(file -> path))
      }
      finally {
        out.close()
      }
    }
  }
}
