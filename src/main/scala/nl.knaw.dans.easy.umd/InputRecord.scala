/**
 * Copyright (C) 2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.umd

import java.io._

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.csv.{ CSVFormat, CSVParser, CSVRecord }
import org.slf4j.{ Logger, LoggerFactory }

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.util.{ Failure, Success, Try }
import resource._

import scala.collection.immutable.Stream.Empty
import scala.language.postfixOps

/** Defaults to the mandatory header line in the CSV file. */
case class InputRecord(recordNr: Long = 1,
                       fedoraID: String = "FEDORA_ID",
                       streamID: String = "STREAM_ID",
                       xmlTag: String = "XML_TAG",
                       oldValue: String = "OLD_VALUE",
                       newValue: String = "NEW_VALUE")

object InputRecord extends DebugEnhancedLogging {

  private val expectedHeader = new InputRecord()
  private val nrOfColumns = expectedHeader.productArity - 1

  def apply(r: CSVRecord) = new InputRecord(
    r.getRecordNumber,
    fedoraID = r.get(0).trim,
    streamID = r.get(1).trim,
    xmlTag = r.get(2).trim,
    oldValue = r.get(3).trim,
    newValue = r.get(4).trim)

  def parse(reader: Reader): ManagedResource[Try[Stream[InputRecord]]] = {
    managed(new CSVParser(reader, CSVFormat.RFC4180))
      .map(_.asScala.toStream match {
        case Empty => Failure(new Exception("no content found in the given file"))
        case head #:: tail if InputRecord(head) == expectedHeader => Try { tail.withFilter(isNotBlank).map(convert) }
        case head #:: _ => Failure(new Exception(s"header line should be: $expectedHeader but was ${ InputRecord(head) }"))
      })
  }

  private def isNotBlank(csvRecord: CSVRecord): Boolean = {
    // calling withIgnoreEmptyLines on the CSVFormat builder would spoil the line numbers
    csvRecord.size != 1 && csvRecord.asScala.headOption.fold(false)(_.trim != "")
  }

  private def convert(csvRecord: CSVRecord): InputRecord = {
    val fields = csvRecord.asScala.toSeq

    def inputReference = s"line ${csvRecord.getRecordNumber}: ${fields.mkString(",")}"

    if (csvRecord.size < nrOfColumns || fields.exists(_.trim.isEmpty))
      throw new Exception(s"incomplete $inputReference")

    val record = InputRecord(csvRecord)

    if (record.oldValue == record.newValue)
      throw new Exception(s"old value equals new value at $inputReference")

    record
  }
}
