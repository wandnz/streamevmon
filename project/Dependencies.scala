import sbt._
import sbt.Keys._

/** Declares the dependencies used by the project, sorted into a number of
  * groups. Also declares the versions in use of all libraries, as well as some
  * API locations for sbt doc.
  */
object Dependencies {
  // Major library
  val flinkVersion = "1.13.1"

  // Used in multiple library declarations
  val chroniclerVersion = "0.6.5"
  val retrofitVersion = "2.9.0"
  val scalaCacheVersion = "0.28.0"
  val log4jVersion = "2.14.1"

  // Used in a single declaration each
  val slf4jVersion = "1.7.30"
  val postgresqlVersion = "42.2.22"
  val squerylVersion = "0.9.16"
  val snakeyamlVersion = "2.3"
  val jacksonVersion = "2.12.3"
  val jgraphtVersion = "1.5.1"

  // Used in tests - scalactic in coreDependencies shares a version with scalatest
  val scalatestVersion = "3.2.9"
  val testcontainersScalaVersion = "0.39.5"

  val providedDependencies: Seq[ModuleID] = Seq(
    // Flink
    "org.apache.flink" %% "flink-scala" % flinkVersion,
    "org.apache.flink" %% "flink-clients" % flinkVersion,
    "org.apache.flink" %% "flink-streaming-scala" % flinkVersion,
    // Logging
    "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion
  ).map(module => module % Provided)

  val coreDependencies: Seq[ModuleID] = Seq(
    // InfluxDB interaction
    // Yes, we have two flavours of IO backend. URL supports chunked queries,
    // while AHC gives us Futures.
    "com.github.fsanaulla" %% "chronicler-ahc-io" % chroniclerVersion,
    "com.github.fsanaulla" %% "chronicler-url-io" % chroniclerVersion,
    "com.github.fsanaulla" %% "chronicler-ahc-management" % chroniclerVersion,
    "com.github.fsanaulla" %% "chronicler-macros" % chroniclerVersion,
    // PostgreSQL interaction
    "org.postgresql" % "postgresql" % postgresqlVersion,
    "org.squeryl" %% "squeryl" % squerylVersion,
    // Caching
    "com.github.cb372" %% "scalacache-core" % scalaCacheVersion,
    "com.github.cb372" %% "scalacache-caffeine" % scalaCacheVersion,
    "com.github.cb372" %% "scalacache-memcached" % scalaCacheVersion,
    // HTTP REST API client
    "com.squareup.retrofit2" % "retrofit" % retrofitVersion,
    "com.squareup.retrofit2" % "converter-jackson" % retrofitVersion,
    // YAML v1.2 configuration parsing
    "org.snakeyaml" % "snakeyaml-engine" % snakeyamlVersion,
    // Type conversion supporting scala
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
    // Tolerant double equality
    "org.scalactic" %% "scalactic" % scalatestVersion,
    // Directed graph logic
    "org.jgrapht" % "jgrapht-core" % jgraphtVersion,
    "org.jgrapht" % "jgrapht-io" % jgraphtVersion,
    // Logging
    "org.apache.logging.log4j" % "log4j-api" % log4jVersion,
    "org.apache.logging.log4j" % "log4j-core" % log4jVersion
  )
    // chronicler-ahc-shared depends on scalatest for some reason, despite not
    // actually needing it for production code.
    .map(_ excludeAll ExclusionRule("org.scalatest", "scalatest").withCrossVersion(CrossVersion.binary))

  val testDependencies: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % scalatestVersion,
    "com.dimafeng" %% "testcontainers-scala" % testcontainersScalaVersion,
    "com.dimafeng" %% "testcontainers-scala-scalatest" % testcontainersScalaVersion,
    "com.dimafeng" %% "testcontainers-scala-postgresql" % testcontainersScalaVersion,
    "com.dimafeng" %% "testcontainers-scala-influxdb" % testcontainersScalaVersion,
    "org.apache.flink" %% "flink-test-utils" % flinkVersion,
    "org.apache.flink" %% "flink-runtime" % flinkVersion classifier "tests",
    "org.apache.flink" %% "flink-streaming-java" % flinkVersion classifier "tests"
  ).map(module => module % Test)

  // These are all dependencies for SMAC2, at the versions distributed with the
  // software. There are more in the parameterTuner/lib/ folder that we couldn't
  // find in Maven. We wanted to reduce the number of raw JARs committed to git,
  // so we get what we can from Maven.
  // Note that we mangled certain jars in the following way to allow for sbt
  // assembly to build us a fat jar for this subproject:
  // DomainInter.jar had its bundled Jama removed due to conflicts with our
  // managed Maven copy.
  // aeatk.jar had its bundled copy of org.apache.commons.io.input.ReaderInputStream
  // removed. It is different to the maven copy, but upon code inspection, the
  // only reference to it is in an example entrypoint, thus it is not needed.
  val tunerDependencies: Seq[ModuleID] = Seq(
    "commons-collections" % "commons-collections" % "3.2.1",
    "commons-io" % "commons-io" % "2.1",
    "org.apache.commons" % "commons-math3" % "3.3",
    "org.apache.commons" % "commons-math" % "2.2",
    "de.congrace" % "exp4j" % "0.3.10",
    "net.objecthunter" % "exp4j" % "0.4.3.BETA-3",
    "com.google.guava" % "guava" % "14.0.1",
    "com.fasterxml.jackson.core" % "jackson-annotations" % "2.3.1",
    "com.fasterxml.jackson.core" % "jackson-core" % "2.3.1",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.3.1",
    "gov.nist.math" % "jama" % "1.0.3", // originally 1.0.2, but evicted by JMathArray dependency
    "net.jcip" % "jcip-annotations" % "1.0",
    "com.github.yannrichet" % "JMathArray" % "1.0",
    "ch.qos.logback" % "logback-access" % "1.1.2",
    "ch.qos.logback" % "logback-core" % "1.1.2",
    "net.sf.opencsv" % "opencsv" % "2.3",
    "org.slf4j" % "slf4j-api" % "1.7.5"
  )

  def builtApiUrl: Option[URL] = Some(url("https://wanduow.github.io/streamevmon/"))

  // This code is from https://stackoverflow.com/a/35673212
  def dependencyApiMappings(classpath: Classpath): Map[File, URL] = {
    def mappingsFor(organization: String, names: List[String], location: String, revision: String => String = identity): Seq[(File, URL)] =
      for {
        entry: Attributed[File] <- classpath
        module: ModuleID <- entry.get(moduleID.key)
        if module.organization == organization
        if names.exists(module.name.startsWith)
      } yield entry.data -> url(location.format(revision(module.revision)))

    val mappings: Seq[(File, URL)] =
      mappingsFor("org.scala-lang", List("scala-library"), "https://scala-lang.org/api/%s/") ++
        mappingsFor(
          "org.apache.flink",
          List("flink-"),
          "https://ci.apache.org/projects/flink/flink-docs-release-%s/api/java/",
          // The URL here only specifies up to minor releases, so we need to
          // drop the patch level of the version, eg 1.12.1 -> 1.12
          revision => revision.substring(0, revision.lastIndexOf('.'))
        ) ++
        mappingsFor(
          "org.apache.logging.log4j", List("log4j-api"),
          "https://logging.apache.org/log4j/2.x/log4j-api/apidocs/index.html"
        ) ++
        mappingsFor(
          "org.apache.logging.log4j", List("log4j-core"),
          "https://logging.apache.org/log4j/2.x/log4j-core/apidocs/index.html"
        )

    mappings.toMap
  }
}
