# Streamevmon

This project is designed as a framework to ingest and process live or historical
time series data, such as the network measurements produced by
[AMP](https://github.com/wanduow/amplet2), and detect anomalies that may be
signs of issues in the network or other data source.

Streamevmon runs as a custom job
within [Apache Flink](https://flink.apache.org/). Flink gives us support for
highly parallel, high-performance computation in a streaming or batch manner, if
desired, while still running effectively on a single host. Custom Flink sources
and sinks have been created to support input and output from files, InfluxDB,
and other locations.

Streamevmon is written in Scala. IntelliJ is usually used for development, but
care has been taken to ensure that everything works with just sbt, the standard
Scala build tool.

## Running streamevmon as a user

As a user, you should use the JAR or .deb release of streamevmon. These are
configured to use the default entrypoint, internally named
[`YamlDagRunner`](src/main/scala/nz/net/wand/streamevmon/runners/unified/YamlDagRunner.scala)
. It requires both a copy of Latency TS I, and an accessible pair of AMP
databases. See [Obtaining Datasets](#obtaining-datasets). By default, the
entrypoint will run a number of detection algorithms on those two datasets.
Detected events will be printed to console, and stored in the InfluxDB instance
configured to read the AMP data from.

See [CONFIGURING_FLOWS](CONFIGURING_FLOWS.md) to learn how to configure this
entrypoint yourself.

If you are using the .deb, you should just need to edit the configuration in
`/etc/streamevmon`, and use `systemctl` to start streamevmon.

If you are using the JAR, you should have a Flink cluster running, and upload it
using the Web UI or command-line tools. Information on deploying Flink can be
found
on [their website](https://ci.apache.org/projects/flink/flink-docs-release-1.12/deployment/)
.

## Obtaining datasets

### Latency TS I

A number of the entrypoints, including most of the single-detector example
runners, rely on the Latency TS I dataset. This can be downloaded for free at
WAND's [WITS](https://wand.net.nz/wits/latency/1/) project. It should be
extracted into the `data/` directory, and the `latency-groundtruth/` directory
renamed to `latency-ts-i`.

This is a simple dataset without a well-studied set of labels to make it useful
for anomaly detection benchmarks, but it has a number of interesting attributes
that can make it useful for detector development.

### AMP

A major target that streamevmon supports for input is
WAND's [Active Measurement Project](https://github.com/wanduow/amplet2). To
access data gathered by AMP, streamevmon needs to be configured to reach the
InfluxDB and PostgreSQL databases that AMP stores its data in.

## Building, Testing, and Packaging

Standard usage of sbt should be sufficient to build streamevmon. Common tasks
include the following:

- `sbt update` will ensure all dependencies are downloaded. This will take a
  while the first time, but the results will be cached for the future.
- `sbt compile` performs incremental compilation.
- `sbt run` will present you with a number of main classes which can be run.
  Most, if not all of these depend on one or more datasets being present. See
  [Obtaining Datasets](#obtaining-datasets) for more details, and the
  [Scaladocs](https://wanduow.github.io/streamevmon/nz/net/wand/streamevmon/runners/index.html)
  for up-to-date information on available entrypoints.
- `sbt test` will run all tests on the main project. Note that many of these
  tests require access to a Docker daemon, so if it is not installed or not
  accessible, some tests will fail.
- `sbt doc` will produce API documentation in the `target/scala-2.12/api`
  directory. This can be opened with a web browser, or
  [viewed online](https://wanduow.github.io/streamevmon/nz/net/wand/streamevmon/index.html)
  .
- To produce a single JAR file with dependencies, as should be loaded into a
  Flink web interface, use `sbt nonProvidedDeps:assembly`. A number of other
  assembly configurations are provided for varying levels of included
  dependencies, or run `sbt assemblyAll` to produce all of them. Supported
  configurations are defined in
  [AssemblyStreamevmonPlugin.autoImport](project/AssemblyStreamevmonPlugin.scala)
  .
- To build a Debian package, use `sbt debian:packageBin`. Note that the
  resulting package depends on our packaged version of Flink, with a matching
  version.

## Extending this project

See [CONTRIBUTING](CONTRIBUTING.md).
