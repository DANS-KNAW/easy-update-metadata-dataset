package nl.knaw.dans.easy.umd

import org.apache.commons.csv.CSVRecord

case class InputRecord(fedoraPid: String, newValue:String)

object InputRecord {
  def apply(csvRecord: CSVRecord) = new InputRecord(csvRecord.get(0), csvRecord.get(1))
}
