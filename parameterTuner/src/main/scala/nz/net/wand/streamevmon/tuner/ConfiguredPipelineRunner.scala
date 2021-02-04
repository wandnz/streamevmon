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

import nz.net.wand.streamevmon.tuner.jobs.Job
import nz.net.wand.streamevmon.tuner.ActorManager.{JobResultHook, RemoveJobResultHook}

import akka.actor.{ActorRef, ActorSystem, Props}

import scala.concurrent.duration.{Duration, MINUTES}
import scala.concurrent.Await

/** This is a helper class for interacting with an ActorSystem, which can
  * provide event-based execution and concurrency.
  */
object ConfiguredPipelineRunner {

  lazy val actorSystem: ActorSystem = ActorSystem("Top-Actor-System")
  lazy val manager: ActorRef = actorSystem.actorOf(Props[ActorManager], "Top-Actor-Manager")

  /** Submit one or more jobs. They will be executed in the background.
    */
  def submit(jobs: Job*): Unit = {
    jobs.foreach(j => manager ! j)
  }

  /** When a job returns, any hooks added by this function will have their apply
    * method called.
    */
  def addJobResultHook(func: JobResultHook): Unit = {
    manager ! func
  }

  /** To remove a JobResultHook, just wrap it in a RemoveJobResultHook object.
    */
  def removeJobResultHook(hook: RemoveJobResultHook): Unit = {
    manager ! hook
  }

  /** This should shut down the ActorSystem, stopping any running jobs and
    * letting the JVM exit.
    */
  def shutdownImmediately(): Unit = Await.result(actorSystem.terminate(), Duration(1, MINUTES))
}
