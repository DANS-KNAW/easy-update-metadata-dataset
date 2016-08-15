package nl.knaw.dans.easy.umd

import java.io.File

import org.apache.commons.csv.{CSVFormat, CSVParser, CSVRecord}
import org.apache.commons.io.Charsets
import scala.collection.JavaConverters.iterableAsScalaIterableConverter

import scala.util.Try

case class InputRecord(fedoraPid: String, newValue:String)

object InputRecord {
  def apply(csvRecord: CSVRecord) = new InputRecord(csvRecord.get(0), csvRecord.get(1))

  def parse(file: File): Try[Stream[InputRecord]] = Try {
    CSVParser.parse(file, Charsets.UTF_8, CSVFormat.RFC4180)
      .asScala
      .filter(_.asScala.nonEmpty)
      .drop(1)
      .toStream
      .map(InputRecord(_))
  }
}
