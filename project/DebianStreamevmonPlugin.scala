import com.typesafe.sbt.packager.debian.DebianPlugin
import com.typesafe.sbt.packager.debian.DebianPlugin.autoImport.Debian
import com.typesafe.sbt.packager.Keys._
import sbt._
import sbt.Keys._
import AssemblyStreamevmonPlugin.autoImport.ProjectAndNonProvidedDeps
import sbtassembly.AssemblyKeys.{assembly, assemblyOutputPath}

/** This plugin specifies our project settings for the DebianPlugin that allows
  * us to package .deb files.
  *
  * It has many non-standard defaults, so a good amount of the work here goes
  * into reapplying the correct standards. We use the native build method that
  * requires debian packaging tools to be installed on the host, since the jdeb
  * build method is missing a number of options and would require even more
  * workarounds.
  */
object DebianStreamevmonPlugin extends AutoPlugin {
  // We rely on the DebianPlugin.
  override def requires = plugins.JvmPlugin && DebianPlugin

  // This plugin must be enabled manually.
  override def trigger = noTrigger

  object autoImport {
    val debianRevision = settingKey[String]("The revision of the debian package for the current program version")
    val debianPatchRelativizeSymlinks = taskKey[Unit]("Make symlinks relative")
    val debianPatchStripGzipTimestamps = taskKey[Unit]("Remove timestamps from .gz files")
  }

  import autoImport._

  lazy val defaultDebianSettings = Seq(
    // Set some basic metadata
    debianRevision := "1",
    version in Debian := s"${version.value}-${debianRevision.value}",
    debianPackageDependencies := Seq("openjdk-11-jre-headless | java11-runtime-headless", "flink-scala2.12"),
    debianPackageProvides := Seq(name.value),
    // deb packages aren't compressed by default by sbt-native-packager since
    // jars are already compressed, but that makes lintian complain and we'd prefer
    // to keep the number of lintian warning overrides at a minimum.
    debianNativeBuildOptions in Debian := Nil,
    // packageBin doesn't normally require the md5sums file to be generated, but we want it.
    packageBin in Debian := ((packageBin in Debian) dependsOn (debianMD5sumsFile in Debian)).value,
    // We also depend on the assembly task that generates the JAR we want.
    packageBin in Debian := ((packageBin in Debian) dependsOn (assembly in ProjectAndNonProvidedDeps)).value,
    // We need to apply a couple of patches to the staged project before packaging it.
    debianPatchRelativizeSymlinks := DebianPatchMethods.RelativizeSymlinks(
      stageDir = s"target/${name.value}-${(version in Debian).value}",
      log = streams.value.log
    ),
    debianPatchStripGzipTimestamps := DebianPatchMethods.StripGzipTimestamps(
      stageDir = s"target/${name.value}-${(version in Debian).value}",
      log = streams.value.log
    ),
    debianPatchRelativizeSymlinks := debianPatchRelativizeSymlinks.triggeredBy(stage in Debian).value,
    debianPatchStripGzipTimestamps := debianPatchStripGzipTimestamps.triggeredBy(stage in Debian).value,
    // Set the file mappings so the plugin knows what to include
    debianChangelog in Debian := Some(file("src/debian/changelog")),
    linuxPackageMappings in Debian ++= DebianPackageMappings.packageMappings(
      baseDirectory = baseDirectory.value,
      jarFile = (assemblyOutputPath in assembly in ProjectAndNonProvidedDeps).value,
      changelogFile = (debianChangelog in Debian).value.get
    ),
    linuxPackageSymlinks in Debian ++= DebianPackageMappings.symlinks
  )

  override lazy val projectSettings: Seq[Def.Setting[_]] = defaultDebianSettings
}
