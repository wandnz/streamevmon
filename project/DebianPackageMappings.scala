import com.typesafe.sbt.packager.linux.LinuxPlugin.autoImport._
import com.typesafe.sbt.packager.linux.LinuxSymlink
import sbt._

/** Defines file mappings from our source tree into the filesystem after the
  * .deb is installed.
  */
object DebianPackageMappings {
  def packageMappings(
    baseDirectory: File,
    jarFile      : File,
    changelogFile: File
  ) = Seq(
    // We've got to include some redundant mappings here since the DebianPlugin
    // creates parent folders with permission 0775, which is non-standard.
    packageTemplateMapping("/etc")().withPerms("0755"),
    packageTemplateMapping("/etc/streamevmon")().withPerms("0755"),
    packageTemplateMapping("/lib")().withPerms("0755"),
    packageTemplateMapping("/lib/systemd")().withPerms("0755"),
    packageTemplateMapping("/lib/systemd/system")().withPerms("0755"),
    packageTemplateMapping("/usr")().withPerms("0755"),
    packageTemplateMapping("/usr/share")().withPerms("0755"),
    packageTemplateMapping("/usr/share/lintian")().withPerms("0755"),
    packageTemplateMapping("/usr/share/lintian/overrides")().withPerms("0755"),
    packageTemplateMapping("/usr/share/doc")().withPerms("0755"),
    packageTemplateMapping("/usr/share/doc/streamevmon")().withPerms("0755"),
    packageTemplateMapping("/usr/share/flink")().withPerms("0755"),
    packageTemplateMapping("/usr/share/flink/lib")().withPerms("0755"),
    packageTemplateMapping("/usr/share/streamevmon")().withPerms("0755"),
    // This is the main jar with all the program code
    packageMapping(
      jarFile -> s"/usr/share/streamevmon/streamevmon.jar"
    ).withPerms("0644"),
    // Systemd unit files
    packageMapping(
      file(s"$baseDirectory/src/debian/streamevmon.service") -> "/lib/systemd/system/streamevmon.service"
    ).withPerms("0644").withConfig(),
    packageMapping(
      file(s"$baseDirectory/src/debian/streamevmon-taskmanager.service") -> "/lib/systemd/system/streamevmon-taskmanager.service"
    ).withPerms("0644").withConfig(),
    // Any override directives for lintian
    packageMapping(
      file(s"$baseDirectory/src/debian/lintian-overrides") -> "/usr/share/lintian/overrides/streamevmon"
    ).withPerms("0644"),
    // We need to manually set where our /usr/share/doc is.
    packageMapping(
      changelogFile -> "/usr/share/doc/streamevmon/changelog.Debian.gz"
    ).gzipped.withPerms("0644"),
    packageMapping(
      file(s"$baseDirectory/src/debian/copyright") -> "/usr/share/doc/streamevmon/copyright"
    ).withPerms("0644"),
    // Default config files
    packageMapping(
      file(s"$baseDirectory/src/main/resources/streamevmon-log4j.properties") -> "/etc/streamevmon/streamevmon-log4j.properties"
    ).withPerms("0644").withConfig(),
    packageMapping(
      file(s"$baseDirectory/src/main/resources/flink-log4j-console.properties") -> "/etc/streamevmon/flink-log4j-console.properties"
    ).withPerms("0644").withConfig(),
    packageMapping(
      file(s"$baseDirectory/src/main/resources/connectorSettings.yaml") -> "/etc/streamevmon/connectorSettings.yaml"
    ).withPerms("0644").withConfig(),
    packageMapping(
      file(s"$baseDirectory/src/main/resources/detectorSettings.yaml") -> "/etc/streamevmon/detectorSettings.yaml"
    ).withPerms("0644").withConfig(),
    packageMapping(
      file(s"$baseDirectory/src/main/resources/generalSettings.yaml") -> "/etc/streamevmon/generalSettings.yaml"
    ).withPerms("0644").withConfig(),
    packageMapping(
      file(s"$baseDirectory/src/main/resources/flows.yaml") -> "/etc/streamevmon/flows.yaml"
    ).withPerms("0644").withConfig()
  )

  // This symlink gets relativized by our patch method. It needs to be here
  // so Flink (as distributed in the .deb we make) can include our jar on its
  // classpath.
  val symlinks = Seq(
    LinuxSymlink("/usr/share/flink/lib/streamevmon.jar", "/usr/share/streamevmon/streamevmon.jar")
  )
}
