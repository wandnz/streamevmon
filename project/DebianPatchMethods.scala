import java.io.File
import java.nio.file._

import com.typesafe.sbt.packager.chmod
import sbt.Logger

import scala.collection.JavaConverters._
import scala.sys.process._

/** These methods correct a few of the mistakes the DebianPlugin makes, bringing
  * them in line with the Debian packaging standards. They should be triggered
  * after the (stage in Debian) task.
  */
object DebianPatchMethods {

  /** Converts all absolute symlinks into relative ones. */
  object RelativizeSymlinks {
    def apply(stageDir: String, log: Logger): Unit = {
      // Grab all the absolute symlinks in the build dir
      Files
        .walk(Path.of(stageDir))
        .iterator.asScala
        .filter(Files.isSymbolicLink)
        .filter(Files.readSymbolicLink(_).isAbsolute)
        .foreach { existingLink =>
          // Find where they point
          val target = Files.readSymbolicLink(existingLink)
          // Map the target onto the build directory
          val targetInBuildDir = Paths.get(stageDir, target.toString)
          // Turn it into a relative target
          val relativized = existingLink.getParent.relativize(targetInBuildDir)
          // Recreate the link as a relative one
          Files.delete(existingLink)
          Files.createSymbolicLink(existingLink, relativized)
          log.info(s"Relativizing symlink at $existingLink from $target to $relativized")
        }
    }
  }

  /** Recompresses any .gz files, removing their timestamps. */
  object StripGzipTimestamps {
    def apply(stageDir: String, log: Logger): Unit = {
      // Grab all the .gz files in the build dir
      Files
        .walk(Path.of(stageDir))
        .iterator.asScala
        .filter(Files.isRegularFile(_))
        .filter(_.toString.endsWith(".gz"))
        // We don't filter for .gz files that already have their timestamps
        // stripped, because at the time of writing the time taken to recompress
        // is negligible and standing up another InputStream to read the
        // timestamp from the file would take far too long in comparison.
        .foreach { gz =>
          val tempFile = new File(gz.toAbsolutePath.toString + "-notimestamp")
          // Use gzip to re-compress the file with new arguments
          // This is equivalent to $ gzip -cd <file> | gzip -9n > output.gz
          (
            Seq("gzip", "-cd", gz.toAbsolutePath.toString) #|
              Seq("gzip", "-9n") #>
              tempFile
            ).! match {
            case 0 =>
            // Throw an error if gzip returned anything other than a success
            case n => sys.error("Error re-gzipping " + gz + ". Exit code: " + n)
          }
          log.info(s"Re-gzipping $gz without timestamps")
          // Overwrite the original file, and fix its permissions.
          Files.move(tempFile.toPath, gz, StandardCopyOption.REPLACE_EXISTING)
          chmod(gz.toFile, "0644")
        }
    }
  }

}
