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

import java.io.File

import org.apache.commons.csv.{CSVFormat, CSVParser, CSVRecord}
import org.apache.commons.io.Charsets
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.util.Try

/** Defaults to the mandatory header line in the CSV file. */
case class InputRecord(recordNr: Long = 1,
                       fedoraID: String = "FEDORA_ID",
                       streamID: String = "STREAM_ID",
                       xmlTag: String = "XML_TAG",
                       oldValue: String = "OLD_VALUE",
                       newValue: String = "NEW_VALUE")

object InputRecord {

  private val expectedHeader = new InputRecord()
  private val nrOfColumns = expectedHeader.productArity - 1

  val log: Logger = LoggerFactory.getLogger(getClass)

  def apply(r: CSVRecord) = new InputRecord(
    r.getRecordNumber, r.get(0), r.get(1), r.get(2), r.get(3), r.get(4)
  )

  def parse(file: File): Try[Stream[InputRecord]] = Try {

    val csvRecords = CSVParser.parse(file, Charsets.UTF_8, CSVFormat.RFC4180).asScala
    val actualHeader = InputRecord(csvRecords.head)

    if (actualHeader != expectedHeader)
      throw new Exception(s"header line should be: $expectedHeader but was $actualHeader")
    else csvRecords
      .toStream
      .withFilter(isNotBlank)
      .map(convert)
  }

  private def isNotBlank(csvRecord: CSVRecord): Boolean = {
    // calling withIgnoreEmptyLines on the CSVFormat builder would spoil the line numbers
    csvRecord.size != 1 && csvRecord.asScala.head.trim != ""
  }

  private def convert(csvRecord: CSVRecord): InputRecord = {

    val fields = csvRecord.asScala.seq

    def inputReference = s"line ${csvRecord.getRecordNumber}: ${fields.mkString(",")}"

    if (csvRecord.size < nrOfColumns || fields.exists(_.trim.isEmpty))
      throw new Exception(s"incomplete $inputReference")

    val record = InputRecord(csvRecord)

    if (record.oldValue == record.newValue)
      throw new Exception(s"old value equals new value at $inputReference")

    record
  }
}
