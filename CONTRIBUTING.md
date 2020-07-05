# Contributing to Streamevmon

As for building, IntelliJ is the recommended way to develop this project.

All code is located in `src/main/scala`. You are encouraged to also write
additional tests where relevant.

The project makes use of Java's packaging system. See the ScalaDoc documentation
for descriptions of each package to determine where new classes should go.

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
  types, which are serialised more efficiently. Testing has shown this to not
  always be the case, but it is unclear what causes the difference. The Kryo
  serialiser does work fine for most cases, but if a type is not supported it
  will simply initialise it as null.
* The behaviour of a Scala `mutable.Queue` has been observed to be unstable
  afte restoration. The first addition to the queue will instead result in a
  'super-null' value that breaks the data structure, throwing a 
  NoSuchElementException whenever it is accessed. The `ModeDetector` and other
  classes utilising a Queue implement a workaround where they detect restoration
  and recreate the queue. This appears to stop the issue from appearing.

It is very important to test state recovery, such as using a test harness like
`NoDependencyCheckpointingTests`.

## Detection algorithms

To add a new detection algorithm, a new package should be created within
`nz.net.wand.streamevmon.detectors`, with a descriptive name. A high-level
description and configuration details should be placed in the documentation of
the package object. An entrypoint that runs the detector in a Flink environment
should be created in the `nz.net.wand.streamevmon.runners.detectors` package.
See the `mode` package for an example of a reasonably simple stateful detector.

Once your detector is complete, you should also add it to the `UnifiedRunner`.
This will make it run in the default entrypoint for the program, along with the
other enabled detectors. While extending the runner, you can also decide what
type of measurements you want to accept, such as selecting from ICMP, DNS, or
other measurement types, and whether you want lossy measurements to be filtered
out before they get to you. Don't forget to test it from within a standalone
Flink environment with a parallelism greater than 1.

### Windowed Operators

`KeyedProcessFunction`, which most detectors implement, will receive from Flink
a stream of measurements at approximately the rate they are ingested by the
Flink pipeline. The "keyed" part means that each stream of measurements (such
as ICMP results from a particular AMP host to a single destination) will be
processed by a separate instance of the detector. However, if Flink receives
measurements out of order, they will be passed to the detector out of order.
For some detectors, this won't matter too much. For others, a few mitigation
strategies are possible:

* When instantiating your detector, you can provide it as an argument to the
  constructor of `WindowedFunctionWrapper`. This function will reorder any
  measurements received within the TimeWindow provided to it, and pass the
  measurements in the correct order to the detector it contains. In effect, it
  ensures correct ordering within certain time windows, at the cost of some
  latency.
  * Ensure that the TimeWindow is not sliding, as that will result in
    measurement duplication.
  * Handover from one window to another is not foolproof, so if measurements
    are received out-of-order while one window is closing and another is opening,
    they may still arrive at the detector out of order.
* Some functions, such as the DistDiffDetector, can be implemented as windowed
  functions from the beginning. Usually, this is best for detectors that don't
  keep much rolling state derived from the stream of measurements received, but
  exceptions should exist. See the `WindowedDistDiffDetector` for an example,
  and compare it with the `DistDiffDetector`.

## Measurements

Adding a new type of measurement includes a number of steps, depending on how
the measurement is obtained.

### InfluxDB measurement

Measurements from InfluxDB can be gathered either via a subscription as they
arrive in the database, or in the traditional query-based fashion. This section
covers both methods, since both are usually required.

#### Measurement class

* If this is a measurement from a new source program, a new package within
  `measurements` should be created.
* New measurements should be implemented as case classes, where each argument 
  to the class constructor corresponds to a column in the InfluxDB table the
  measurement represents. They should also inherit from Measurement.
* If a field name is desired that does not match the database column, the column
  name can be specified using the `org.squeryl.annotations.Column` annotation.
  While this annotation comes from our PostgreSQL library, it is easiest to use
  it for the same purpose with both databases.
* It is encouraged to use `java.time.Instant` for time fields, but otherwise the
  datatypes used should match those in the database. Use `Option` when fields
  might be omitted.
* If your measurement can report lossy results, such as an ICMP test that did
  not receive any responses, implement the `isLossy` function from Measurement.
  Otherwise, just implement it to unconditionally return `false`.
* Feel free to create additional case classes to represent groups of fields or
  derived values, but do not use a single field to represent multiple database
  columns. This should instead be implemented via a lazy val which constructs
  your class from the database columns.

#### Measurement companion object

A companion object which extends `MeasurementFactory` should be created. This is
where you should declare relevant case classes, additional constructors, and
other static methods.

* You should override `table_name` with the name of the measurement as it appears
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
  * Add a reference to this function in the `MeasurementFactory` object, in
    `createMeasurement` so that your measurement type is supported by this
    generic factory.
  * Don't forget to add some line protocol examples in `SeedData` and tests for
    this function in `MeasurementCreateTest`.

#### InfluxSchema

`nz.net.wand.streamevmon.connectors.InfluxSchema` contains the `InfluxReader`
classes used to parse the results of queries for historical data. Currently, 
the results are given as `org.typelevel.jawn.ast.JValue` subclasses, which must
be converted to plain Scala types manually.

* The structure of an InfluxReader definition should be self-evident from the
  existing parsers. There has, at time of writing, been no evidence of a
  requirement to implement `readUnsafe`, so we leave it unimplemented.
* It is recommended to make use of the `js.get(cols.indexOf("name"))` pattern,
  for readability and maintainability, especially if you later decide to reorder
  the fields of your measurement.
* Also make use of `nullToOption`. Unfortunately, the existing parsers are not
  very type-safe, but if the schema of the tables they represent does not change,
  there should not be any issues.
* Add a function to utilise the reader in `InfluxHistoryConnection`.
* Tests for these readers should go in `InfluxHistoryConnectionTest`.

## SourceFunctions

A SourceFunction is the way that Flink ingests records from external programs,
such as InfluxDB. As such, a new SourceFunction might be required if you add a
new type of measurement from a program that isn't currently supported, or perhaps
a new delivery mechanism, such as a new type of database.

The following covers the scenario where the desired measurements are stored in
InfluxDB, but come from a new table or program.

* `InfluxSourceFunction` is the base class which handles both subscription and
  historical data from InfluxDB. It should be inherited, very similarly to
  `nz.net.wand.streamevmon.flink.AmpMeasurementSourceFunction`.
* You will likely want to add a new section into `default.properties`
  corresponding to the table your source is pulling from.
* A new runner should be created in the `runners` package. This is useful both
  for testing and if you want to just use your new SourceFunction without the
  extra complexity that the UnifiedRunner introduces.
* You should also add your SourceFunction as a new source in the `UnifiedRunner`.
  Make sure that the configured `subscriptionName` for your configuration
  datatype is set to something unique, as well as the source's name and uid.
