/* This file is part of streamevmon.
 *
 * Copyright (C) 2020-2021  The University of Waikato, Hamilton, New Zealand
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
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nz.net.wand.streamevmon.tuner

import nz.net.wand.streamevmon.TestBase
import nz.net.wand.streamevmon.tuner.jobs.{Job, JobResult, SimpleJob}
import nz.net.wand.streamevmon.tuner.ActorManager.{JobResultHook, RemoveJobResultHook}

import scala.concurrent.{Await, Future, TimeoutException}
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.ExecutionContext.Implicits.global

class AkkaTest extends TestBase {
  "Akka wrappers" should {
    "execute code when submitted to" in {
      var jobHasExecuted = false

      ConfiguredPipelineRunner.submit(new Job("test-job") {
        private val self: Job = this

        override def run(): JobResult = {
          jobHasExecuted = true
          new JobResult {
            override val job: Job = self
          }
        }
      })

      Thread.sleep(1000)
      assert(jobHasExecuted)
    }

    "execute job result hooks" in {
      var hookHasExecuted = false

      ConfiguredPipelineRunner.addJobResultHook(
        _ => hookHasExecuted = true
      )

      ConfiguredPipelineRunner.submit(SimpleJob("hello-world"))

      // This will wait until hookHasExecuted goes true, or for 1 second, whichever comes first.
      try {
        Await.result(
          Future(while (!hookHasExecuted) {}),
          Duration(1, SECONDS)
        )
      }
      catch {
        case _: TimeoutException =>
        case e: Throwable => throw e
      }

      assert(hookHasExecuted)
    }

    "remove job result hooks" in {
      var hookHasExecuted = false

      val hook = new JobResultHook {
        override def apply(jr: JobResult): Unit = hookHasExecuted = true
      }

      ConfiguredPipelineRunner.addJobResultHook(hook)
      ConfiguredPipelineRunner.removeJobResultHook(RemoveJobResultHook(hook))

      ConfiguredPipelineRunner.submit(SimpleJob("hello-world"))

      try {
        Await.result(
          Future(while (!hookHasExecuted) {}),
          Duration(1, SECONDS)
        )
      }
      catch {
        case _: TimeoutException =>
        case e: Throwable => throw e
      }
      assert(!hookHasExecuted)
    }
  }
}
