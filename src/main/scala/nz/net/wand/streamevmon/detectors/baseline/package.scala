package nz.net.wand.streamevmon.detectors

/** This package contains the baseline detector module. It accepts any type of
  * measurement which includes HasDefault, and outputs Baseline Events.
  *
  * ==Configuration==
  *
  * This package is configured by the `detector.baseline` config key group.
  *
  * - `maxHistory`: The maximum number of measurements to retain. This can
  * affect how sensitive the detector is to changes that do not last long.
  * Default 50.
  *
  * - `percentile`: Which percentile of the measurement window to check for
  * changes. For example, a value of 0.50 is the median, while 0.75 is the value
  * which is larger than 75% of the other values in the window.
  * Default 0.10.
  *
  * - `threshold`: The change severity threshold that must be exceeded for an
  * event to be emitted. This value should be between 0 and 100.
  * Default 25.
  *
  * - `inactivityPurgeTime`: If the gap between two concurrent measurements is
  * this many seconds or more, we should drop all our history and start fresh.
  * Default 600.
  *
  */
package object baseline {}
