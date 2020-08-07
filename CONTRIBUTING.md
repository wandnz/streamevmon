# Contributing to Streamevmon

In this document, it is assumed that you will be using IntelliJ with the Scala 
plugin.

All code is located in `src/main/scala`. You are encouraged to also write
additional tests for new or modified code where possible.

## Detection algorithms

To add a new detection algorithm, a new package should be created within
`nz.net.wand.streamevmon.detectors`, with a descriptive name. A high-level
description and configuration details should be placed in the documentation of
the package object. An entrypoint that runs the detector in a Flink environment
should be created in the `nz.net.wand.streamevmon.runners.detectors` package.
See the `mode` package for an example of a reasonably simple stateful detector.

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

## Measurements

Adding a new type of measurement includes a number of steps, depending on how
the measurement is obtained.

### All measurement types

If this is a measurement from a new source type, a new package within
`measurements` should be created. You may also want to create a new parent
class, like `EsmondMeasurement`.

Any Measurement can also inherit from `HasDefault` or `CsvOutputable`. These
provide extra methods for detectors to use if they don't know the concrete
type of a measurement. HasDefault is particularly useful, and is recommended to
use if possible.

* If your measurement can report lossy results, such as an ICMP test which 
  received no responses, implement the `isLossy` function from Measurement.
  Otherwise, just implement it to unconditionally return `false`.
* `toCsvFormat` should be implemented similarly to the following:

  `override def toCsvFormat: Seq[String] = DNS.unapply(this).get.productIterator.toSeq.map(toCsvEntry)`
  
  Note that case classes don't get an unapply() method if they have more than
  22 fields. See `RichDNS` for an example of how to implement this without
  using unapply.
* Feel free to create additional case classes to represent groups of fields, or
  add additional derived values.

### InfluxDB measurement

Measurements from InfluxDB are well-supported, so adding them is likely to be
fairly straightforward. All InfluxMeasurements inherit from HasDefault and
CsvOutputable.

#### Measurement class

* New measurements should be implemented as case classes, where each argument 
  to the class constructor corresponds to a column in the InfluxDB table the
  measurement represents. They should also inherit from `InfluxMeasurement`.
* If a field name is desired that does not match the database column, the column
  name can be specified using the `org.squeryl.annotations.Column` annotation.
  While this annotation comes from our PostgreSQL library, it is easiest to use
  it for the same purpose with both databases.
* `java.time.Instant` is used for time fields. Other fields (except `stream`)
  should have datatypes matching those in the database. Use `Option` when fields
  might be omitted.

#### Measurement companion object

A companion object which extends `InfluxMeasurementFactory` should be created.

* You should override `table_name` with the name of the table as it appears
  in InfluxDB.
* You should override `columnNames` as follows, where T is your measurement type:  
  `override def columnNames: Seq[String] = getColumnNames[T]`
* You should override `create` with a function that returns an `Option` of your
  measurement type. **This is the function which produces your measurement from
  an InfluxDB subscription!**
  * A few helper functions are defined to make this function simpler. The
    function should look like the following:  
    ```scala
    val data = splitLineProtocol(subscriptionLine)
    if (data.head != table_name) {
      None
    }
    else {
      Some(
        YourMeasurementType(
          getNamedField(data, "stream").get.toInt,
          Instant.ofEpochMilli(TimeUnit.NANOSECONDS.toMillis(data.last.toLong))
        )
      )
    }
    ```  
  * `splitLineProtocol` and `getNamedField` effectively turn the line protocol
    string given to us by InfluxDB into a `Map[String,Option[String]]`.
  * Checking that the first entry is the same as `table_name` enforces that you
    are parsing an entry from the correct table.
  * String values are surrounded by quotes, which you must deal with manually via
    `.map(_.drop(1).dropRight(1))` (for optional fields).
  * Integer values have a suffix of `i`, which should be removed before parsing.
  * Add a reference to this function in the `InfluxMeasurementFactory` object, in
    `createMeasurement` so that your measurement type is supported by this
    generic factory.
  * Don't forget to add some line protocol examples in `SeedData` and tests for
    this function in `MeasurementCreateTest`.

#### InfluxSchema

`nz.net.wand.streamevmon.connectors.influx.InfluxSchema` contains definitions
for the `InfluxReader` classes used to parse the results of queries for 
historical data. The results are given as `org.typelevel.jawn.ast.JValue` 
subclasses, which must be converted to plain Scala types manually.

* The structure of an InfluxReader definition should be self-evident from the
  existing parsers. There has been no evidence of a requirement to implement 
  `readUnsafe`, so we leave it unimplemented.
* It is recommended to make use of the `js.get(cols.indexOf("name"))` pattern,
  as well as `nullToOption`. This is for readability and maintainability, 
  especially if you later decide to reorder the fields of your measurement.
* Add a function to utilise the reader in `InfluxHistoryConnection`.
* Tests for these readers should go in `InfluxHistoryConnectionTest`. 

## Important notes on statefulness and serialisation with Flink 

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

* Serialising lambdas does not appear to be supported.
* While the Flink documentation discusses the treatment of case classes as POJO
  types, which are serialised more efficiently, testing has shown this to not
  always be the case. It is unclear what causes this difference. The Kryo
  serialiser does work fine for most cases, but it is recommended to test your
  code to ensure that fields are not re-initialised as null when they are not
  expected to be.
* The behaviour of a Scala `mutable.Queue` has been observed to be unstable
  afte restoration. The first addition to the queue will instead result in a
  'super-null' value that breaks the data structure, throwing a 
  NoSuchElementException whenever it is accessed. The `ModeDetector` and other
  classes utilising a Queue implement a workaround where they detect restoration
  and recreate the queue. This appears to stop the issue from appearing.

It is very important to test state recovery, such as using a test harness like
`NoDependencyCheckpointingTests`. It can also be useful to test your code in a
standalone Flink environment with a parallelism greater than 1.

## Adding support to the main entrypoint

The main entrypoint uses classes in the `runners.unified.schema` package to
build instances of sources, detectors, and sinks as required. These need to have
support specifically added for them to work. See 
[CONFIGURING_FLOWS.md](CONFIGURING_FLOWS.md) for details on how the config file
is structured.

Whenever you need to add new values to an Enumeration, note that the string you
pass as an argument to `Value`, `ValueBuilder`, or other equivalent classes
will become the name your type is referred to with in configuration files. It
must be unique within the enum.

* Sources (SourceFunctions or FileInputFormats) have two relevant files: 
  `SourceType` and `SourceSubtype`. If a SourceType, like `influx`, has 
  subtypes, they must be specified for a config to be valid. The logic to 
  construct a source should be placed in SourceSubtype if subtypes are 
  supported, or SourceType otherwise. In either case, add new values of type 
  `ValueBuilder` to the relevant class.
* Sinks are even simpler. Just add a new `ValueBuilder` and some construction
  logic to `SinkType`.
* Measurement types should be added to `SourceDatatype`. These new values should
  be added to both functions in `DetectorInstance`, and the `typedAs` definition
  in `StreamToTypedStreams`.
  * Note the structure of `typedAs`: It defines what measurement types each
    source type can generate. You will need to specify a SourceType that matches
    your SourceDatatype.
* Detectors should be added as new `ValueBuilder`s within `DetectorType`.
  * Next, add support for your detector in `ValueBuilder`'s `buildKeyed` method.
  * You should declare your Measurement trait dependencies here in the same
    style as the existing detectors. If you declared the input type of your
    detector as `MeasT <: Measurement with HasDefault`, you should check that
    the `hasDefault` parameter is defined *before* constructing your detector,
    and throw an exception with a useful error message otherwise.
