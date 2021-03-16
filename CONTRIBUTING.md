# Contributing to streamevmon

This document is indended to be a guide to extending streamevmon with additional
measurement types, detection algorithms, sources, and sinks. It does not address
more complex topics like writing new entrypoints.

Note that all contributions to this project fall under the GPL-3 as included
in [COPYING](COPYING).

All code is located in `src/main/scala`. You are highly encouraged to also write
additional tests for new or modified code where possible, which should be in
`src/test/scala`. See [Writing tests](#writing-tests) for details.

## Measurements

There are a number of steps common to all measurement types. A few types of
measurement, such as those obtained from InfluxDB, are already supported, and
will be easier to add using the existing traits.

Note that all measurements must be serializable, and any transient or derived
data should be marked as such using a `def` or the `@transient` annotation. See
[Notes on statefulness and serialization with Flink](#notes-on-statefulness-and-serialisation-with-flink)
for details.

### All measurement types

If this is a measurement from a new source type, a new package within
`measurements` should be created. You may also want to create a new parent class
in which to put common inherited traits, similar to
[`InfluxMeasurement`](src/main/scala/nz/net/wand/streamevmon/measurements/traits/InfluxMeasurement.scala)
.

All measurements (or their common parent trait) should inherit from
[`Measurement`](src/main/scala/nz/net/wand/streamevmon/measurements/traits/Measurement.scala)
. This lets us perform polymorphism and provides a number of helpful fields and
functions.

The `traits` package also has a few extra traits that any Measurement can
extend, such as `HasDefault` (which you should use if possible).

Most measurements should be implemented as case classes, since this has a few
extra useful features, like an `unapply` method.

### Extra notes for InfluxMeasurements

When implemented as case classes, each argument to the class constructor
directly corresponds to a column in the InfluxDB table the measurement
represents. If a field name is desired that doesn't match the database column,
the column name can be specified using the `org.squeryl.annotations.Column`
annotation. While this comes from our PostgreSQL library, it is easiest to use
it for the same purpose with both databases.

Use `java.time.Instant` for time fields. If fields might be omitted from a
particular entry, wrap them in an `Option`. Other fields should have datatypes
matching those in the database column.

### Supporting realtime InfluxDB measurements

You should define a companion object which extends
[`InfluxMeasurementFactory`](src/main/scala/nz/net/wand/streamevmon/measurements/traits/InfluxMeasurementFactory.scala)
. The companion object should override `columnNames` as follows if it produces
the correct results, where `T` is your measurement type:

```scala
override def columnNames: Seq[String] = getColumnNames[T]
```

`create` is the function which produces your measurement from an InfluxDB
subscription, so it is important to keep it reasonably light. For examples, see
the following:

- [`ICMP`](src/main/scala/nz/net/wand/streamevmon/measurements/amp/ICMP.scala)
  is a simple function without many fancy tricks.
- [`DNS`](src/main/scala/nz/net/wand/streamevmon/measurements/amp/DNS.scala)
  shows a number of different basic data types.
- [`Flow`](src/main/scala/nz/net/wand/streamevmon/measurements/bigdata/Flow.scala)
  shows some more complex data types.

Note that integer types are followed by an `i` which must be dropped before
parsing. String types are surrounded with quotes, so you need to drop one
character from either side.

Once this is complete, add a reference to your new function in the
`InfluxMeasurementFactory` object's `createMeasurement` method, and write some
tests. Most of the existing measurement-creation tests store examples in
[`SeedData`](src/test/scala/nz/net/wand/streamevmon/test/SeedData.scala).

### Supporting historical InfluxDB measurements

Add a new entry
to [`InfluxSchema`](src/main/scala/nz/net/wand/streamevmon/connectors/influx/InfluxSchema.scala)
, following the existing examples. Then, add a function that uses this reader to
[`InfluxHistoryConnection`](src/main/scala/nz/net/wand/streamevmon/connectors/influx/InfluxHistoryConnection.scala)
, following the example of `getIcmpData`.

## Detection algorithms

To add a new detection algorithm, a new package should be created within
`nz.net.wand.streamevmon.detectors`, with a descriptive name. A high-level
description and configuration details should be placed in the documentation of
the package object. An entrypoint that runs the detector in a Flink environment
should be created in the `nz.net.wand.streamevmon.runners.detectors` package.
See the `mode` package for an example of a reasonably simple stateful detector.

All detectors must implement
[`HasFlinkConfig`](src/main/scala/nz/net/wand/streamevmon/flink/HasFlinkConfig.scala)
, obtaining their configuration by using `configWithOverride(getRuntimeContext)`
.

Once your detector is complete, you should also add support for it to
`runners.unified.schema`. This will allow it to be used by the default
entrypoint. See [the relevant section](#adding-support-to-the-main-entrypoint)
for details.

### Windowed Operators

Most detectors will be implemented as a `KeyedProcessFunction`. These receive
measurements from Flink in effectively real-time. Each stream (referred to by
a key) will be processed by a separate instance of your detector. Users can
instead choose to use your detector as a windowed function, which receives
groups of measurements instead.

This is done automatically if not supported by your detector. A time window will
be used, with the user's configuration applied. Alternatively, you can create
your own implementation as a `ProcessWindowFunction`, which allows you more
flexibility in how you use these groups of measurements. You can choose what
kind of window your windowed function uses when integrating it with the default
entrypoint.

## Sources and Sinks

Sources should go in the `flink.sources` package, while sinks go in
`flink.sinks`. Currently, we do not use subpackages in these locations, but this
may change in the future.

Sources will usually implement `RichSourceFunction`, since it gives additional
features that let you interact with the runtime state. Similarly, sinks should
implement `RichSinkFunction`.

All sources and sinks must implement
[`HasFlinkConfig`](src/main/scala/nz/net/wand/streamevmon/flink/HasFlinkConfig.scala)
, obtaining their configuration by using `configWithOverride(getRuntimeContext)`
.

Generally, code that interacts with outside systems should be placed in the
`connectors` package, in its own subpackage. There are no particular
requirements for structure in this package, just ensure that any package objects
are documented with the connector's configuration and other implementation
details, and the API is reasonably tidy. This is to allow flexibility in what
libraries can be used here.

As with detectors, sources and sinks must have support added to
`runners.unified.schema` before it can be used by the default entrypoint. See
[the relevant section](#adding-support-to-the-main-entrypoint) for details.

## Adding support to the main entrypoint

The main entrypoint uses classes in the `runners.unified.schema` package to
build instances of sources, detectors, and sinks as required. These need to have
support specifically added for them to work. See
[CONFIGURING_FLOWS.md](CONFIGURING_FLOWS.md) for details on how the config file
is structured.

Whenever you need to add new values to an Enumeration, note that the string you
pass as an argument to `Value`, `ValueBuilder`, or other equivalent classes will
become the name your type is referred to with in configuration files. It must be
unique within the enum.

* Sources (SourceFunctions or FileInputFormats) have two relevant files:
  [`SourceType`](src/main/scala/nz/net/wand/streamevmon/runners/unified/schema/SourceType.scala)
  and [`SourceSubtype`](src/main/scala/nz/net/wand/streamevmon/runners/unified/schema/SourceSubtype.scala)
  . If a SourceType, like `influx`, has subtypes, they must be specified for a
  config to be valid. The logic to construct a source should be placed in
  `SourceSubtype` if subtypes are supported, or `SourceType` otherwise. In
  either case, add new values of type `ValueBuilder` to the relevant class.
  * Note that a `FileInputFormat` should also return a `FilePathFilter` to
    determine which files it should read.
* Sinks are even simpler. Just add a new `ValueBuilder` and some construction
  logic to
  [`SinkType`](src/main/scala/nz/net/wand/streamevmon/runners/unified/schema/SinkType.scala)
  .
* Measurement types should be added to
  [`SourceDatatype`](src/main/scala/nz/net/wand/streamevmon/runners/unified/schema/SourceDatatype.scala)
  . These new values should be added to both functions in
  [`DetectorInstance`](src/main/scala/nz/net/wand/streamevmon/runners/unified/schema/DetectorInstance.scala)
  , and the `typedAs` definition in
  [`StreamToTypedStreams`](src/main/scala/nz/net/wand/streamevmon/runners/unified/schema/StreamToTypedStreams.scala)
  .
  * Note the structure of `typedAs`: It defines what measurement types each
    source type can generate. You will need to specify a SourceType that matches
    your SourceDatatype.
* Detectors should be added as new `ValueBuilder`s within
  [`DetectorType`](src/main/scala/nz/net/wand/streamevmon/runners/unified/schema/DetectorType.scala)
  .
  * Next, add support for your detector in `ValueBuilder`'s `buildKeyed` method.
  * If your detector has a windowed variant, add support to `buildWindowed`.
  * You should declare your Measurement trait dependencies here in the same
    style as the existing detectors. If you declared the input type of your
    detector as `MeasT <: Measurement with HasDefault`, you should check that
    the `hasDefault` parameter is defined *before* constructing your detector,
    and throw an exception with a useful error message otherwise.

## Notes on statefulness and serialisation with Flink

Flink includes mechanisms for restoring state after a crash, meaning any
detectors, SourceFunctions, or other parts of the pipeline that rely on
persistent state must use their system in order to function correctly. An
outline can be found [here](https://ci.apache.org/projects/flink/flink-docs-stable/dev/types_serialization.html).
An example of the usage of these tools is the `ModeDetector`. Take particular
note of the `ValueState` type fields, the fact that the class extends
`KeyedProcessFunction`, and the `open` function.

While the serialisation process generally works quite well despite many classes
having to fall back to the slower Kryo serialiser, it has a few weaknesses that
must be kept in mind.

* You must implement `CheckpointedFunction` in order to support restoration.
* Serialising lambdas does not appear to be supported.
* While the Flink documentation discusses the treatment of case classes as POJO
  types, which are serialised more efficiently, testing has shown this to not
  always be the case. It is unclear what causes this difference. The Kryo
  serialiser does work fine for most cases, but it is recommended to test your
  code to ensure that fields are not re-initialised as null when they are not
  expected to be.
* The behaviour of a Scala `mutable.Queue` has been observed to be unstable afte restoration. The first addition to the queue will instead result in a
  'super-null' value that breaks the data structure, throwing a
  NoSuchElementException whenever it is accessed. The `ModeDetector` and other
  classes utilising a Queue implement a workaround where they detect restoration
  and recreate the queue. This appears to stop the issue from appearing.

It is very important to test state recovery, such as using a test harness like
`NoDependencyCheckpointingTests`. It can also be useful to test your code in a
standalone Flink environment with a parallelism greater than 1.

## Writing tests

It's highly encouraged to write tests for new code where possible. Most new
tests can be made based on existing ones in existing files. Check the package
under `src/test/scala` corresponding to the files you edited when adding your
code.

Most of the existing tests for detectors are under
`detectors.checkpointing`, but consider adding a new package to test your own
detector if it has any components which need it. You should also add a new entry
in `runners.unified.DetectorBuildTest`.

Sources and sinks don't have a lot of existing functionality tests, since we use
dedicated interfaces with the external systems they read from and write to which
are tested instead. Those tests are in the `connectors` package. You should
still test sources and sinks for checkpointing functionality if they use it, as
well as adding a new entry for each new type and subtype in
[`runners.unified.SourceBuildTest`](src/test/scala/nz/net/wand/streamevmon/runners/unified/SourceBuildTest.scala)
or `SinkBuildTest`.

New measurements should have their creation and any enrichment methods tested in
the `measurements` package.

Note that we have a number of convenience traits for writing tests, and make
use of [TestContainers](https://github.com/testcontainers/testcontainers-scala) 
for testing a connector's integration with an external system where possible.
