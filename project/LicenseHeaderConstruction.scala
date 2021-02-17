import de.heikoseeberger.sbtheader.CommentCreator
import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._

import scala.compat.Platform.EOL
import scala.language.implicitConversions
import scala.util.matching.Regex

object LicenseHeaderConstruction {
  val YearPattern: Regex = "(?s).*?(\\d{4}(-\\d{4})?).*".r
  val AuthorPattern: Regex = "(?s).*?Author: (.*?)(\r?\n|\r).*".r
  val DoubleNewline: String = s"$EOL *$EOL * "

  val OrganisationName: String = "The University of Waikato, Hamilton, New Zealand"
  val DefaultAuthorNotice: String = "Author: <unknown>"
  val ProjectNameNotice: String = "This file is part of streamevmon."
  val AllRightsReserved: String = "All rights reserved."
  val WandNotice: String = "This code has been developed by the University of Waikato WAND" + EOL + " * " +
    "research group. For further information please see https://wand.nz," + EOL + " * " +
    "or our Github organisation at https://github.com/wanduow"

  def commentCreator(currentYear: String): CommentCreator = new CommentCreator() {
    override def apply(text: String, existingText: Option[String]): String = {
      existingText
        .getOrElse(HeaderCommentStyle.cStyleBlockComment.commentCreator.apply(text, existingText))
        .applyWandFormat(currentYear)
        .trim
    }
  }

  /** Matches against the YearPattern, returning the year range if found.
    */
  def findYear(header: String): Option[String] = {
    header match {
      case YearPattern(years, _) => Some(years)
      case _ => None
    }
  }

  /** Matches against the AuthorPattern, returning the name of the author(s) if
    * found.
    */
  def findAuthor(header: String): Option[String] = {
    header match {
      case AuthorPattern(author, _) => Some(author)
      case _ => None
    }
  }

  /** Extension methods for String to allow fluent syntax for applying
    * modifications to a file header comment. Use `applyWandFormat` to run
    * everything in an order that works.
    */
  implicit class HeaderFixers(val textToFix: String) extends AnyVal {
    /** Updates the year in a header message to the current correct value.
      * If there is a single year present, but it is not the current year,
      * update it to "20xx-CurrentYear".
      * If there is a range present, only the most recent year will be updated
      * to CurrentYear.
      */
    implicit def fixYear(currentYear: String): String = findYear(textToFix) match {
      case Some(year) =>
        val yearToUse = if (year.contains("-")) {
          if (year.split("-")(1) != currentYear) {
            s"${year.split("-").head}-$currentYear"
          }
          else {
            year
          }
        }
        else {
          if (year != currentYear) {
            s"$year-$currentYear"
          }
          else {
            year
          }
        }
        textToFix.replace(year, yearToUse)
      case None => textToFix
    }

    /** Retains the author note that appears just after the Copyright line.
      * Also adds the AllRightsReserved and WandNotice strings, since it's
      * easier to do it here since we're already affecting the position we want
      * them.
      */
    implicit def fixAuthorAndStaticNotices: String = findAuthor(textToFix) match {
      case Some(_) => textToFix
      case None => textToFix.replace(
        OrganisationName,
        OrganisationName + DoubleNewline +
          DefaultAuthorNotice + DoubleNewline +
          AllRightsReserved + DoubleNewline +
          WandNotice
      )
    }

    /** Adds the ProjectNameNotice to the top of the file. */
    implicit def fixProjectName: String = {
      if (textToFix.contains(ProjectNameNotice)) {
        textToFix
      }
      else {
        // Magic number is enough to appropriately insert the double newline.
        textToFix.patch("/*".length, s" $ProjectNameNotice$DoubleNewline", 4)
      }
    }

    /** Applies all fixes in an appropriate order. */
    implicit def applyWandFormat(currentYear: String): String = {
      textToFix
        .fixYear(currentYear)
        .fixAuthorAndStaticNotices
        .fixProjectName
    }
  }

}
