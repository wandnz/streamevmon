# Streamevmon

This project is designed as a framework to ingest and process live or historical
network measurement data, such as that produced by AMP, and detect anomalies
that may be signs of issues in the network.

Apache Flink is used as an engine, as it facilitates highly parallel,
high-performance computation in a streaming or batch manner. Custom sources have
been created to read data from CSV files (such as those included in the Latency
TS I dataset), or receive live measurements from the InfluxDB instance that an
AMP cluster is storing data in. A custom sink exists which can store detected
events into an InfluxDB instance (the same as the source instance, or another),
so that the events can later be visualised by a system like Grafana.

The project is written in Scala, and uses IntelliJ as a development environment,
with sbt as a build tool.

## Building and Testing

The easiest way to build this project is to open it in IntelliJ, which should
be configured with the Scala plugin.

It should be enough to simply open the folder with IntelliJ, make sure that SBT
changes are imported and scalafmt is downloaded, and click the Build button.
The default entrypoint is the UnifiedEntrypoint, which requires a running and
accessible instance of InfluxDB (and PostgreSQL if RichMeasurements are used).
The locations of these databases can be configured using
`conf/streamevmon.properties`.

To run automated tests, right-click on the test/scala/nz.net.wand.streamevmon/
folder and select "Run ScalaTests in 'streamevmon'". You should have the ability
to run Docker containers for many of these tests to work. Some tests are
sensitive to timing and may work if the test suite is ran a second time.

To generate documentation, use the menu item "Tools > Generate Scaladoc".

## Packaging

To generate a JAR file to run on a real Flink cluster, go to the sbt shell
(View > Tool Windows > sbt shell), wait for it to initialise, and run
`assembly`. This will output to target/scala-2.12.

This will generate a JAR file with all the required dependencies included.
You can then upload it to a running Flink cluster. Be sure that your Flink
instance is running the same versions of Flink and Scala as you built this
project for - currently Scala 2.12 and Flink 1.9.1.

## Extending this project

Extending this project is fairly straightforward. All code can be found in
`src/main/scala`. You are encouraged to also write additional tests if you add
new code.

The project has a few main packages, and some classes which belong to the base
package. See the ScalaDoc documentation for descriptions of the packages.

To add a new detection algorithm, a new package should be created within
`nz.net.wand.streamevmon.detectors`, with a descriptive name. A high-level
description and configuration details should be placed in the documentation of
the package object. Finally, an entrypoint that runs the detector in a Flink
environment should be created in the `nz.net.wand.streamevmon.runners` package.
See the `mode` package for an example of a reasonably simple stateful detector.

Once your detector is complete, you should also add it to the `UnifiedRunner`.
This will make it run in the default entrypoint for the program, along with the
other enabled detectors. While extending the runner, you can also decide what
type of measurements you want to accept, such as selecting from ICMP, DNS, or
other measurement types, and whether you want lossy measurements to be filtered
out before they get to you.
