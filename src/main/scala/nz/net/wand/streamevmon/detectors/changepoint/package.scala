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

package nz.net.wand.streamevmon.detectors

/** This package contains the changepoint detector module.
  *
  * The main class is [[nz.net.wand.streamevmon.detectors.changepoint.ChangepointProcessor ChangepointProcessor]],
  * while the class to be instantiated for use with Flink is [[nz.net.wand.streamevmon.detectors.changepoint.ChangepointDetector ChangepointDetector]].
  *
  * It accepts any type of measurement which includes HasDefault, and outputs
  * Changepoint Events.
  *
  * It implements a changepoint detector as outlined by Ryan Prescott Adams
  * and David J.C. MacKay in the paper "Bayesian Online Changepoint Detection"
  * ([[https://arxiv.org/abs/0710.3742v1]]). It also owes thanks to the
  * implementation included in netevmon written by Richard Sanger. This version
  * was written by Daniel Oosterwijk.
  *
  * ==Configuration==
  *
  * This package is configured by the `detector.changepoint` config key group.
  *
  * - `maxHistory`: The maximum number of runs to retain. This affects how long
  * measurements will continue to affect the outcome of the probability
  * distributions used internally.
  * Default 60.
  *
  * - `triggerCount`: The number of abnormal measurements that must be observed
  * in a row before we believe a changepoint has happened. High numbers will
  * increase detection latency, but might make the algorithm more resilient to
  * short-lived changes.
  * Default 40.
  *
  * - `ignoreOutlierNormalCount`: The number of normal measurements that must
  * occur after some outliers are seen before we ignore the outliers and return
  * to the state of the detector before those outliers occurred.
  * Default 1.
  *
  * - `inactivityPurgeTime`: If the gap between two successive measurements is
  * this many seconds or more, we should drop all our history and start fresh.
  * If this value is set to 0, the behaviour is disabled.
  * Default 60.
  *
  * - `minimumEventInterval`: Detected events must be at least this many seconds
  * apart in order to be emitted.
  * Default 10.
  *
  * - `severityThreshold`: When a changepoint is detected, its severity must be
  * higher than this number (0-100) to be emitted. Severity is calculated based
  * on the magnitude of the change: If the median changes from 200 to 800, or
  * from 800 to 200, the severity will be 75. (3/4, since the change has a
  * magnitude of 4x).
  * Default 30.
  *
  */
package object changepoint {}
