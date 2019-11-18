package nz.net.wand.streamevmon.detectors

/** Simple loss detector. Emits events when a certain proportion of recent
  * values are lossy. This detector is best used on streams you expect to barely
  * ever have loss, since it is naive, sensitive, and noisy.
  *
  * Written by Daniel Oosterwijk, and loosely based on the loss detector
  * included in netevmon, which was written by Shane Alcock and Brendon Jones.
  *
  * ==Configuration==
  *
  * This package is configured by the `detector.loss` config key group.
  *
  * - `maxHistory`: The maximum number of measurements to retain.
  * Default 30.
  *
  * - `lossCount`: The number of lossy measurements that must occur within the
  * last maxHistory number of measurements before an event is emitted.
  * Default 10.
  *
  * - `consecutiveCount`: The number of consecutive lossy measurements before
  * an event is emitted.
  * Default 5.
  */
package object loss {}
