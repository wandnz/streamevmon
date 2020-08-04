package nz.net.wand.streamevmon.detectors

/** Spike detector. Emits events when new measurements are considerably
  * different from recent measurements.
  *
  * Notably, this is not a detector of changes in mean. It performs best on
  * sudden spikes, as the slow ramp up that can often be observed with changes
  * in mean tends to cause this algorithm to ignore them.
  *
  * Access the `"detailed-output"` side output stream for detailed info, which
  * can be used for debugging and tuning.
  *
  * See [[nz.net.wand.streamevmon.detectors.spike.SmoothedZScore SmoothedZScore]]
  * for sources and citations.
  *
  * ==Configuration==
  *
  * This package is configured by the `detector.spike` config key group.
  *
  * - `inactivityPurgeTime`: The amount of time between events, in seconds,
  * before history is purged.
  * Default 60.
  *
  * - `lag`: The lag of the moving window (i.e. how big the window is). Similar
  * to the `maxHistory` parameter of other detectors, but this name was kept
  * for consistency with the original algorithm.
  * Default 50.
  *
  * - `threshold`: The z-score at which the algorithm signals (i.e. how many
  * standard deviations away from the moving mean a peak (or signal) is).
  * Default 30.0.
  *
  * - `influence`: The influence (between 0 and 1) of new signals on the mean
  * and standard deviation (how much a peak (or signal) should affect other
  * values near it).
  * Default 0.01.
  *
  */
package object spike {}
