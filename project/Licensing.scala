import com.typesafe.sbt.SbtLicenseReport.autoImportImpl._
import com.typesafe.sbt.license.{DepModuleInfo, LicenseInfo}
import de.heikoseeberger.sbtheader.CommentStyle
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._
import sbt._
import sbt.Keys._

object Licensing {

  import LicenseHeaderConstruction._

  implicit val CurrentYear: String = java.time.Year.now.getValue.toString

  lazy val commentStyle: CommentStyle = HeaderCommentStyle
    .cStyleBlockComment
    .copy(
      commentCreator = commentCreator(CurrentYear)
    )

  lazy val sharedLicensing: Seq[SettingsDefinition] = Seq(
    organizationName := OrganisationName,
    headerMappings := headerMappings.value ++ Map(
      HeaderFileType.scala -> commentStyle,
      HeaderFileType.java -> commentStyle
    )
  )

  lazy val coreLicensing: Seq[SettingsDefinition] = Seq(
    startYear := Some(2021), // we only began licensing this code in 2021, but it was started in 2019
    licenses += (HeaderLicense.GPLv3OrLater.spdxIdentifier, url("https://www.gnu.org/licenses/gpl-3.0.html"))
  ) ++ sharedLicensing

  lazy val parameterTunerLicensing: Seq[SettingsDefinition] = Seq(
    startYear := Some(2020),
    // This module inherits the AGPL from SMAC 2.10.x. The core module is not affected.
    licenses += (HeaderLicense.AGPLv3OrLater.spdxIdentifier, url("https://www.gnu.org/licenses/agpl-3.0.html"))
  ) ++ sharedLicensing

  /** Applies license overrides for those missed by sbt-license-report */
  def applyLicenseOverrides = {
    licenseOverrides := {
      case DepModuleInfo("org.apache.zookeeper", "zookeeper", _) => LicenseInfo(LicenseCategory.Apache, "Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0.txt")
    }
  }
}
