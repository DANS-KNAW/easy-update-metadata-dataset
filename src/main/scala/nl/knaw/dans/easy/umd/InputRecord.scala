/**
 * Copyright (C) 2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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

case class InputRecord(fedoraPid: String, newValue: String, oldValue: String)

object InputRecord {

  val log: Logger = LoggerFactory.getLogger(getClass)

  def apply(csvRecord: CSVRecord) = new InputRecord(csvRecord.get(0), csvRecord.get(1), csvRecord.get(2))

  private val expectedHeaders = InputRecord("FEDORA_ID", "NEW_VALUE", "OLD_VALUE")

  def parse(file: File): Try[Stream[InputRecord]] = Try {
    val csvRecords = CSVParser.parse(file, Charsets.UTF_8, CSVFormat.RFC4180).asScala
    val actualHeader = InputRecord(csvRecords.head)
    if (actualHeader != expectedHeaders)
      throw new Exception(s"header should be: $expectedHeaders but was $actualHeader")
    else csvRecords
      .toStream
      .withFilter(csvRecord => // skip empty lines
        csvRecord.asScala.size != 1 && csvRecord.asScala.head.trim != ""
      )
      .map{ csvRecord =>
        if(csvRecord.asScala.size < 3  || csvRecord.asScala.seq.map(_.trim.isEmpty).toSet.contains(true))
          throw new Exception(s"incomplete line: $csvRecord")
        InputRecord(csvRecord)
      }
  }
}
