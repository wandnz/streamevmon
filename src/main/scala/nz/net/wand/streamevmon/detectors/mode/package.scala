package nz.net.wand.streamevmon.detectors

/** Mode detector. Emits events when the value that occurs the most often in
  * recent events changes significantly. This detector will re-scale the values
  * of measurements in order to perform some bucketing before mesauring modes.
  *
  * Written by Daniel Oosterwijk, and is a close parallel to the mode detector
  * included in netevmon, which was written by Shane Alcock and Brendon Jones.
  *
  * ==Configuration==
  *
  * This package is configured by the `detector.mode` config key group.
  *
  * - `maxHistory`: The number of recent measurements to retain and consider.
  * Default 30.
  *
  * - `minFrequency`: The minimum number of times a particular value must occur
  * in recent history to be considered for an event.
  * Default 6.
  *
  * - `minProminence`: The minimum difference between the number of instances of
  * the primary and secondary modes in recent history for the primary mode to
  * be considered for an event.
  * Default 3.
  *
  * - `threshold`: The minimum threshold value used when considering the absolute
  * difference between the values of the old mode and the new mode. This may be
  * higher in practice, since the detector automatically calculates a threshold
  * and takes the minimum of the two values.
  * Default 7.5.
  *
  * - `inactivityPurgeTime`: The amount of time between events, in seconds,
  * before history is purged.
  * Default 60s.
  *
  */
package object mode {}
