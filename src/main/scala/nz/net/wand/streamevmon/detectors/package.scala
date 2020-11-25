package nz.net.wand.streamevmon

/** Contains implementations of detection algorithms. Many of these will be
  * implemented as ProcessFunctions, similarly to the
  * [[nz.net.wand.streamevmon.detectors.SimpleThresholdDetector SimpleThresholdDetector]].
  *
  * Generally, a detection algorithm will want to take some stream of
  * [[nz.net.wand.streamevmon.measurements.traits.Measurement Measurements]]
  * (keyed, windowed, or otherwise) and produce a stream of
  * [[nz.net.wand.streamevmon.events.Event Events]].
  *
  * See CONTRIBUTING.md for details.
  */
package object detectors {}
