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
  * TODO: Update this once the configuration is implemented.
  *
  */
package object changepoint {}
