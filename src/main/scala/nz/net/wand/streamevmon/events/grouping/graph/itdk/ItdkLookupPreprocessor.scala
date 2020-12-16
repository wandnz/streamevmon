package nz.net.wand.streamevmon.events.grouping.graph.itdk

import nz.net.wand.streamevmon.connectors.postgres.schema.AsNumberCategory

import java.io._
import java.net.InetAddress

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import scala.collection.mutable
import scala.io.Source

object ItdkLookupPreprocessor {
  /** Produces a file where each line includes a single IP, and the node it
    * belongs to.
    */
  def invertItdkNodeLookup(nodesFile: File, asLookupFile: File): File = {
    val invertedMapFile = new File(s"${nodesFile.getCanonicalPath}.inverted")
    val sourceStream = Source.fromFile(nodesFile)
    val invertedMapWriter = new BufferedWriter(new FileWriter(invertedMapFile))

    val asLookupReader = new BufferedReader(new FileReader(asLookupFile))
    var lastAsLine = asLookupReader.readLine()
    var asLineNodeId = lastAsLine.split(" ")(1).drop(1)

    sourceStream.getLines
      // The file starts with some comments about how CAIDA made the file, which
      // we don't care about
      .dropWhile(_.startsWith("#"))
      // Force scala to act sequentially to prevent clobbering as we write
      .seq
      .foreach { line =>
        // Split the node ID and IPs
        val parts = line.split(":  ")
        // Get the associated AS number, if we know it
        val asn = {
          while (asLineNodeId.toInt < parts.head.drop(6).toInt) {
            lastAsLine = asLookupReader.readLine()
            asLineNodeId = lastAsLine.split(" ")(1).drop(1)
          }
          if (asLineNodeId != parts.head.drop(6)) {
            AsNumberCategory.Unknown.id // == 0
          }
          else {
            lastAsLine.split(" ")(2).toInt
          }
        }

        // Then for each IP, write it to the file with the node ID
        parts.last.split(" ").seq.foreach { ip =>
          // we drop the "node N" part of the ID, since it's the same for all.
          invertedMapWriter.write(s"$ip ${parts.head.drop(6)} $asn")
          invertedMapWriter.newLine()
        }
      }
    invertedMapWriter.close()
    sourceStream.close()
    asLookupReader.close()

    invertedMapFile
  }

  /** Creates a new sorted copy of the given file. */
  def sortFile(file: File): File = {
    // we use bash because it handles memory complexity better than we can, and
    // doing the sort in java just causes out of memory exceptions most of the
    // time - this is way easier
    import scala.sys.process._
    val sortedFile = new File(s"${file.getAbsolutePath}.sorted")
    (s"sort -V ${file.getAbsolutePath}" #> sortedFile).!
    sortedFile
  }

  /** Turns a sorted inverted map file (from invertItdkLookup -> sortFile) into
    * a binary format that's much smaller and easier to search through. Also
    * returns a file containing a map of the first octet of an IP to the
    * distance (0.0-1.0) through the aligned file that entries beginning with
    * that octet ends.
    */
  def createAlignedInvertedMapFile(
    sortedInvertedMapFile: File,
    onlyProduceLookup    : Boolean = false
  ): (File, File) = {
    val alignedFile = new File(s"${sortedInvertedMapFile.getCanonicalPath}.aligned")
    val sortedStream = Source.fromFile(sortedInvertedMapFile)
    val alignedWriter = if (onlyProduceLookup) {
      new DataOutputStream(OutputStream.nullOutputStream())
    }
    else {
      new DataOutputStream(new BufferedOutputStream(new FileOutputStream(alignedFile)))
    }

    // Start the map off by filling in all the keys we'll ever find.
    val countByFirstOctet = mutable.Map[Byte, Long]()
    Range(0, 256).foreach(i => countByFirstOctet(i.toByte) = 0)

    // Each line in the sorted file corresponds to one entry in the aligned file
    sortedStream.getLines
      // do it sequentially...
      .seq
      .foreach { pair =>
        // Each line contains IP address and node ID
        // Each half of the output entry is four bytes
        val parts = pair.split(" ")
        val ip = InetAddress.getByName(parts.head).getAddress
        val node = parts(1).toInt
        val asn = parts(2).toInt

        countByFirstOctet(ip.head) += 1

        alignedWriter.write(ip)
        alignedWriter.writeInt(node)
        alignedWriter.writeInt(asn)
      }
    alignedWriter.close()
    sortedStream.close()

    // Create a cumulative distribution mapping with the counts of first octet
    // values we gathered while making the aligned file.
    val cumulativeDist = {
      var accumulator: Double = 0
      val totalCount = countByFirstOctet.values.sum

      countByFirstOctet.toSeq.map(kv => (kv._1 & 0xff, kv._2)).sortBy(_._1).map { case (k, v) =>
        val oldAccumulator = accumulator

        accumulator += v.toDouble / totalCount
        if (accumulator > 1.0) {
          accumulator = 1.0
        }

        (k, oldAccumulator)
      }.toMap
    }

    // Write out the cumulative distribution mapping to disk as JSON.
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

    val cumulativeDistFile = new File(s"${alignedFile.getCanonicalPath}.lookupHelper.json")
    val jsonOut = new FileWriter(cumulativeDistFile)
    mapper.writeValue(jsonOut, cumulativeDist)
    jsonOut.close()

    (alignedFile, cumulativeDistFile)
  }

  /** Warning: Expensive function!
    *
    * Converts an ITDK <>.nodes file into a format that allows us to quickly
    * look up the node ID that an IP belongs to.
    *
    * If the `cleanup` parameter is false, it also leaves behind the textual
    * inverted form of the file, and its sorted equivalent.
    *
    * Returns a tuple with the aligned file and its corresponding cumulative
    * lookup file, which is a JSON-serialized Map[Int,Double].
    */
  def preprocess(nodesFile: File, cleanup: Boolean = false): (File, File) = {
    val invertedMapFile = invertItdkNodeLookup(nodesFile, new File(s"${nodesFile.getCanonicalPath}.as"))
    val sortedFile = sortFile(invertedMapFile)
    val (alignedFile, cumulativeDistFile) = createAlignedInvertedMapFile(sortedFile)

    if (cleanup) {
      invertedMapFile.delete()
      sortedFile.delete()
    }

    (alignedFile, cumulativeDistFile)
  }

  def main(args: Array[String]): Unit = {
    println(preprocess(new File(args(0))))
  }
}
