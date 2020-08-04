package nz.net.wand.streamevmon.detectors

/** The Distribution Difference detector keeps track of two sequential sets of
  * recent measurements. When it determines that the measurements form
  * distributions which are significantly different, it emits an event.
  *
  * Written by Daniel Oosterwijk, and is a close parallel to the detector
  * included in netevmon, which was written by Shane Alcock and Brendon Jones.
  *
  * This package includes two versions of the detector: One takes a keyed stream,
  * and the other takes a windowed keyed stream. They are configured identically.
  * When using the UnifiedRunner or the DistDiffRunner, the configuration option
  * `detector.distdiff.useFlinkWindow` can be used to choose the windowed variant
  * (true) or the sequential variant (false).
  *
  * ==Configuration==
  *
  * This package is configured by the `detector.distdiff` config key group.
  *
  * - `recentsCount`: The number of recent measurements to retain and consider.
  * Default 20.
  *
  * - `minimumChange`: The amount of proportional change between the sums of the
  * distributions that must be observed before an event is considered. For
  * example, the default value of 1.05 indicates a 5% minimum difference.
  *
  * - `zThreshold`: The minimum z-value indicating distribution difference that
  * must be observed before an event is considered. This number doesn't appear
  * to have much absolute meaning, but it is useful for tuning sensitivity.
  * Default 5.0.
  *
  * - `dropExtremeN`: When analysing the two sets of measurements, they are first
  * sorted, then this many entries are dropped from the high and low ends in
  * order to ignore outliers.
  * Default 2.
  *
  * - `inactivityPurgeTime`: The amount of time between events, in seconds,
  * before history is purged.
  * Default 1200s.
  *
  */
package object distdiff {}
