# Streamevmon

This project is designed as a framework to ingest and process live or historical
network measurement data, such as that produced by AMP, and detect anomalies
that may be signs of issues in the network.

Apache Flink is used as an engine, as it facilitates highly parallel,
high-performance computation in a streaming or batch manner. Custom sources and
sinks have been created to support input and output from files, InfluxDB
instances, and other locations. 

The project is written in Scala, and uses IntelliJ as a development environment,
with sbt as a build tool. There are some helper scripts that are usually in
Python. With IntelliJ Ultimate (which can be obtained with an educational
account), these can be run from within the editor.

## Building and Testing

The easiest way to build this project is to open it in IntelliJ, which should
be configured with the Scala plugin.

It should be enough to simply open the folder with IntelliJ and wait. SBT should
automatically download itself and the required dependencies, then IntelliJ will
begin to index them along with our source files. Once all that is done (or at
any point while you're waiting), building the project should work. 

To run automated tests, right-click on the test/scala/nz.net.wand.streamevmon/
folder in the Project toolbar and select "Run ScalaTests in 'streamevmon'". You 
need the ability to run Docker containers for many of these tests to work.

To generate documentation, use the menu item "Tools > Generate Scaladoc".

### Configuring Python scripts

If using IntelliJ Ultimate with the Python plugin, you have the option of
running the Python scripts in the editor rather than externally. This needs some
setup to work properly. This section walks you through how to set up a 
virtualenv environment specific to this project.

To start setting up the Python environment, browse to any Python file in the
scripts/ folder, and click `Configure Python Interpreter` in the top bar which
appears. Alternatively, browse to `File` > `Project Structure`.

Go to `SDKs`, click the `+` icon, and select `Add Python SDK...`

Select `Virtualenv Environment` -> `New Environment`, and input your favourite 
location and interpreter. The code has only been tested with Python 3.8. venv/ 
and .venv/ are currently in the .gitignore, so are good choices. Regardless of
the location, it should not be checked into Git.

Go back to `Modules` -> `+` icon -> `Import Module` -> Select the .iml file in 
`scripts/`.

If there are errors, select the `Streamevmon-scripts` module, go to the 
`Dependencies` tab, and select the `Module SDK` corresponding to your virtualenv.

Hit OK and go back to the Python source file. The bar at the top will change to
one asking you to install the package's required dependencies. Install all of
them. Since you're in a virtualenv, this will be isolated from the rest of your
system. It might take a while, since we install packages like `pandas` and 
`scikit-learn`.

## Packaging

To generate a JAR file to run on a real Flink cluster, go to the sbt shell
(View > Tool Windows > sbt shell), wait for it to initialise, and run
`assembly`. This will output to `target/scala-2.12`.

This will generate a JAR file with all the required dependencies included.
You can then upload it to a running Flink cluster. Be sure your Flink
instance is running the same versions of Flink and Scala as you built this
project for - these can be double-checked at the top of [build.sbt](build.sbt).

To create a JAR with only the program code, go to `build.sbt` and uncomment the
following line near the bottom:

`assembly / assemblyOption := (assembly / assemblyOption).value.copy(includeScala = false, includeDependency = false)`

To create a JAR with only the dependencies (useful for creating a new Flink
Docker image with required dependencies baked in), run `assemblyPackageDependency`.

## Extending this project

See [CONTRIBUTING](CONTRIBUTING.md).

## Running this project

The default entrypoint is the YamlDagRunner, which requires a running and
accessible instance of InfluxDB which an AMP cluster is using as storage. It
also needs a copy of Latency TS I to be present in the `data/` folder. See
[CONFIGURING_FLOWS](CONFIGURING_FLOWS.md) to learn how to use this entrypoint.
