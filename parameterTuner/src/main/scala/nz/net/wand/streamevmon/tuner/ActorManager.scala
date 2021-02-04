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

import nz.net.wand.streamevmon.Logging
import nz.net.wand.streamevmon.tuner.jobs.{Job, JobActor, JobResult}
import nz.net.wand.streamevmon.tuner.ActorManager.{JobResultHook, RemoveJobResultHook}

import akka.actor.{Actor, Props}

import scala.collection.mutable

/** This is the top-level Actor of our ActorSystem. It controls dispatch
  * and results for all jobs in the parameterTuner module. You should not use
  * this class directly - [[ConfiguredPipelineRunner]] has methods for that.
  *
  * Code which would allow us to shutdown only after all jobs are finished can
  * be found at https://stackoverflow.com/a/48766551, but we don't currently
  * support that.
  */
class ActorManager extends Actor with Logging {

  /** All hooks are executed whenever a job result is received.
    * As such, they should probably have some sort of filtering or matching
    * involved.
    */
  val jobResultHooks: mutable.Buffer[JobResultHook] = mutable.Buffer()

  /** Called when any message is received. Depending on the item, different
    * behaviour is triggered.
    *
    * - A [[nz.net.wand.streamevmon.tuner.jobs.Job Job]] will be executed.
    * - A [[nz.net.wand.streamevmon.tuner.jobs.JobResult JobResult]] will be
    * passed to all the [[ActorManager.JobResultHook JobResultHooks]] registered.
    * - A [[ActorManager.JobResultHook JobResultHook]] will be registered to be
    * called when a JobResult is received.
    * - A [[ActorManager.RemoveJobResultHook RemoveJobResultHook]] will cause
    * the associated JobResultHook to be removed.
    */
  override def receive: Receive = {
    case j: Job =>
      // Each child goes to a new JobActor, which will execute that job and
      // then exit.
      logger.info(s"Spawning child for job $j")
      val child = context.actorOf(Props[JobActor], j.toString)
      child ! j

    case jr: JobResult =>
      // Each Job will return exactly one JobResult.
      logger.info(s"Got job result $jr")
      jobResultHooks.foreach(_ (jr))

    case func: JobResultHook =>
      // A JobResultHook hangs around until it's removed.
      logger.info(s"Adding job result hook")
      jobResultHooks.append(func)

    case remove: RemoveJobResultHook =>
      // A RemoveJobResultHook contains a reference to a JobResultHook by which
      // we can remove the hook.
      val hook = jobResultHooks.zipWithIndex.find(_._1 == remove.jrh)
      hook match {
        case Some(value) =>
          logger.info(s"Removing job result hook ${value._1.getClass.getCanonicalName}")
          jobResultHooks.remove(value._2)
        case None =>
          logger.warn(s"Failed to remove job result hook ${remove.jrh.getClass.getCanonicalName}")
      }
  }
}

object ActorManager {

  abstract class JobResultHook {
    def apply(jr: JobResult): Unit
  }

  case class RemoveJobResultHook(jrh: JobResultHook)
}
