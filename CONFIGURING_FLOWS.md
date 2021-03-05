# Configuring the Default Entrypoint

The default entrypoint can be found at `runners.unified.YamlDagRunner`. If no
additional files are provided, it will produce a default Flink pipeline which
will likely be good enough for most scenarios, provided the right data inputs
are accessible.

This document describes how to provide your own pipeline configuration.

## Configuring Defaults

Any file with a `.yaml` or `.yml` extension in the `./conf/` folder except
`flows.yaml` and `flows.yml` will be read as a config file. Configuration
options are described throughout this project's Scaladoc, but a summary is
provided here.

All config comes under a few top-level keys, depending on the category of the
module you're configuring.

* `caching`:
  See [Scaladoc](https://wanduow.github.io/streamevmon/nz/net/wand/streamevmon/Caching.html)
  .
* `flink`: This key group is mainly used by the YamlDagRunner, but certain
  options may also be used to inform behaviour in other places.
  * `flink.maxLateness`: The number of seconds a measurement may be out-of-order
    by before it is determined to be late, and dropped. This is used to assign
    watermarks so Flink can process them correctly. Default 20.
  * `flink.checkpointInterval`: How often, in seconds, to snapshot the current
    state to allow failures to be resumed. Default 600.

* `source`: Configuration for SourceFunctions and FileInputFormats. See
  [Scaladoc](https://wanduow.github.io/streamevmon/nz/net/wand/streamevmon/flink/sources/index.html)
  .
* `sink`: Configuration for SinkFunctions. See
  [Scaladoc](https://wanduow.github.io/streamevmon/nz/net/wand/streamevmon/flink/sinks/index.html)
  .
* `detector`: Configuration for detectors. See
  [Scaladoc](https://wanduow.github.io/streamevmon/nz/net/wand/streamevmon/detectors/index.html)
  .

Note that none of these keys are pluralised, which differs from the `flows.yaml`
configuration format.

## Configuring the Pipeline

First, create a new file at `conf/flows.yaml`. Note that `conf/flows.yml`, with
the extension `.yml` instead of `.yaml`, will not work. Any files in this folder
other than those two are considered general config settings, and
the [above section](#configuring-defaults) applies to them.

`flows.yaml` represents a Directed Acyclic Graph (DAG) describing the flow of
data from sources, through detectors, and into sinks. For a config to make
sense, there must be at least one item of each category. All the following
sections can have as many keys as desired.

Sources and sinks are only constructed if they are required by a detector. As
such, any sources or sinks which are declared but not required will not appear
on the Flink execution graph.

In addition to the keys described in the following sections, you can declare
any top-level keys you desire. These will be ignored by the parser, but you can
use them to take advantage of YAML features like anchors and aliases.

### Sources

The `sources` top-level key defines SourceFunctions and FileInputFormats. 
The structure is as follows:

```yaml
source-name:
  type: source-type
  subtype: source-subtype
  config:
    anyConfigKey: value
```

* `source-name` is a custom name for the source which will show up in the Flink
  dashboard and is used to refer to the source in the rest of the config file.
* `source-type` generally refers to the kind of system the source interacts with:
  `influx` or `esmond` are examples.
* The `subtype` key is optional, unless the source type has subtypes. For 
  example, the `influx` type has subtypes including `amp` and `bigdata`, one of
  which must be specified.
* The `config` key is optional, and described in 
  [Overriding Configurations](#overriding-configurations).

### Sinks

The structure of the `sinks` key is as follows:

```yaml
sink-name:
  type: sink-type
  config:
    anyConfigKey: value
```

Sinks are declared similarly to sources, but there are currently no sinks with
subtypes.

### Detectors

While the source and sink definitions are merely declarations, the detector
definitions both declare detectors and define how they are connected to the
declared sources and sinks. The structure is as follows:
 
```yaml
detector-name:
  type: detector-type
  instances:
    - source:
        - name: source-name
          datatype: source-datatype
          filterLossy: true
      sink:
        - name: sink-name-1
        - name: sink-name-2
      config:
        anyConfigKey: value
```

This looks a little confusing at first glance. Let's break it down.

* As with sources and sinks, the detector-name can be anything you want. It will
  show up on the execution graph.
* All detectors must have a type. This defines the algorithm being used.
* A source is referenced by the custom name you gave it. You must also specify
  a datatype which you want to receive from it. Unfortunately there is currently
  no way to receive all datatypes from a particular source without declaring all
  of them as separate instances. Finally, the `filterLossy` key allows you to
  ignore lossy measurements, such as ICMP tests which received no responses.
* Declaring a connection to a sink is as simple as specifying the custom name
  you gave it.
* The `config` key is optional, and described in 
  [Overriding Configurations](#overriding-configurations).
* You can declare several instances of the same detector. The `instances` key
  expects a list of mappings. You would declare multiple instances as follows:
  
  ```yaml
  instances:
    - source:
        - <source config>
      sink:
        - <sink config>
      config:
        - <config options>
    - source:
        - <source config>
      sink:
        - <sink config>
      config:
        - <config options>
  ```
  
  Notice how each instance has its own list of sources, sinks, and config 
  options. You can configure each instance uniquely: the only thing they share
  is a name and type.

### Overriding Configurations

Any element can have configuration applied to it in the flow declaration. These
are applied per-instance, after any configurations from the other files in
`conf/` are applied.

This allows detailed individual configuration of many instances of the same type
of detector. For example, a baseline detector might choose to have a different 
`maxHistory` value for ICMP and DNS measurements.

The `config` key is optional wherever it is valid. Its structure is identical to 
that used in the other `conf/` files, meaning you should see the ScalaDoc for
the relevant item to determine supported config options. Generally, these are
just a flat list of key-value pairs.
