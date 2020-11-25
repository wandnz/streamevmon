package nz.net.wand.streamevmon.measurements

/** This package contains traits that Measurements and related objects may
  * implement. These provide additional functionality, and many detectors
  * require that input measurements implement one or more of these traits.
  *
  * Particularly notable entries include the following:
  *
  * - `Measurement` must be implemented by all measurements.
  * - `HasDefault` is required by many detectors, as it simplifies delivery of
  * a simple time series by allowing measurements to offer a simple default
  * Double value.
  * - `InfluxMeasurement` and `PostgresMeasurement` define certain other traits
  * that are common to all measurements stored in particular databases
  * (particularly in the context of AMP).
  * - `MeasurementMeta` represents metadata about a measurement stream.
  */
package object traits {}
