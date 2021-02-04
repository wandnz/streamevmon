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

package nz.net.wand.streamevmon.tuner.jobs

import nz.net.wand.streamevmon.Logging

import org.slf4j.{Logger, LoggerFactory}

/** Parent class for jobs to be executed by a [[JobActor]]. Should be submitted
  * to the [[nz.net.wand.streamevmon.tuner.ActorManager ActorManager]] via the
  * [[nz.net.wand.streamevmon.tuner.ConfiguredPipelineRunner ConfiguredPipelineRunner]].
  */
abstract class Job(uid: String) extends Logging {

  override protected lazy val logger: Logger = LoggerFactory.getLogger(s"${getClass.getName}:$uid")

  override def toString: String = s"Job-$uid"

  /** This is the entrypoint for the job, and is likely to be excuted in a
    * separate thread to the one that submitted the job.
    */
  def run(): JobResult
}
