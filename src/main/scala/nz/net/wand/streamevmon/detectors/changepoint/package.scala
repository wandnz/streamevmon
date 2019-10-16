package nz.net.wand.streamevmon.detectors

/** This package contains all the code for the changepoint detector module.
  *
  * The main class is [[nz.net.wand.streamevmon.detectors.changepoint.ChangepointProcessor ChangepointProcessor]],
  * while the class to be instantiated for use with Flink is [[nz.net.wand.streamevmon.detectors.changepoint.ChangepointDetector ChangepointDetector]].
  *
  * It accepts any type of measurement, and outputs [[nz.net.wand.streamevmon.events.ChangepointEvent ChangepointEvents]].
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
  * Default 20.
  *
  * - `triggerCount`: The number of abnormal measurements that must be observed
  * in a row before we believe a changepoint has happened. High numbers will
  * increase detection latency, but might make the algorithm more resilient to
  * short-lived changes.
  * Default 10.
  *
  * - `ignoreOutlierNormalCount`: The number of normal measurements that must
  * occur after some outliers are seen before we ignore the outliers and return
  * to the state of the detector before those outliers occurred.
  * Default 1.
  *
  * - `inactivityPurgeTime`: If the gap between two concurrent measurements is
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
