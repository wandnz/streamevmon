package nz.net.wand.streamevmon

/** This package contains the optional parameterTuner module. It uses SMAC2 to
  * generate new sets of parameters according to some method of scoring results.
  *
  * The main entrypoint is [[nz.net.wand.streamevmon.tuner.ParameterTuner ParameterTuner]].
  *
  * The `jobs` and `strategies` packages contain logic for parameter tuning
  * independent from SMAC. The `nab` package contains logic for using the NAB
  * benchmark to get scores from a particular algorithm run. The `nab.smac`
  * package contains the glue between the NAB scorer and SMAC.
  *
  * Currently, only NAB is supported.
  */
package object tuner {}
