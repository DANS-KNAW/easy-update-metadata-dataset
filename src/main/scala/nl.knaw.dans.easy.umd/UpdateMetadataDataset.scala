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

import java.io.{ File, FileInputStream, Reader }
import java.nio.charset.{ Charset, CodingErrorAction, StandardCharsets }

import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import resource.{ ManagedResource, Using, managed }

import scala.io.Source
import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, Node, PrettyPrinter }

object UpdateMetadataDataset extends DebugEnhancedLogging {

  def run(implicit ps: Parameters): Try[Unit] = {
    val fedora = FedoraStreams(fedoraCredentials = ps.fedoraCredentials)

    Try { testFriendlyRun(fedora).acquireAndGet(identity) }
      .flatten
      .doIfSuccess(_ => logger.info("All completed succesfully"))
      .doIfFailure { case e => logger.error(s"Failures : ${ e.getMessage }", e) }
  }

  def testFriendlyRun(fedora: FedoraStreams)(implicit ps: Parameters): ManagedResource[Try[Unit]] = for {
    reader <- textReader(ps.input)
    result <- InputRecord.parse(reader)
      .map(_.flatMap(_.map(update(fedora)).find(_.isFailure).getOrElse(Success(()))))
  } yield result

  def textReader(file: File): ManagedResource[Reader] = {
    val decoder = StandardCharsets.UTF_8.newDecoder()
    decoder.onMalformedInput(CodingErrorAction.REPORT)

    for {
      source <- managed(Source.fromFile(file)(decoder))
      reader <- managed(source.reader())
    } yield reader
  }

  def update(fedora: FedoraStreams)(record: InputRecord)(implicit ps: Parameters): Try[Unit] = {
    logger.info(record.toString)
    for {
      oldXML <- fedora.getXml(record.fedoraID, record.streamID)
      _ <- Transformer.validate(record.streamID, record.xmlTag, record.oldValue, oldXML)
      transformer = Transformer(record.streamID, record.xmlTag, record.oldValue, record.newValue)
      newXML = transformer.transform(oldXML)
      _ <- reportChanges(record, oldXML, newXML)
      _ <- fedora.updateDatastream(record.fedoraID, record.streamID, newXML.toString())
    } yield ()
  }.recoverWith {
    case e => Failure(new Exception(s"failed to process: $record, reason: ${e.getMessage}", e))
  }

  private def reportChanges(record: InputRecord, oldXML: Elem, newXML: Seq[Node]): Try[Unit] = {
    if (oldXML == newXML)
      Failure(new Exception(s"could not find ${record.streamID} <${record.xmlTag}>${record.oldValue}</${record.xmlTag}>"))
    else Try {
      val oldLines = new PrettyPrinter(160, 2).format(oldXML).lines.toList
      val newLines = new PrettyPrinter(160, 2).format(newXML.head).lines.toList

      logger.info(s"old ${record.streamID}: ${compare(oldLines, newLines)}")
      logger.info(s"new ${record.streamID}: ${compare(newLines, oldLines)}")
    }
  }

  /** @return lines of xs not in ys */
  private def compare(xs: List[String], ys: List[String]): String = {
    val diff = xs.diff(ys)
    if (diff.size <= 1)
      diff.mkString("", "", "")
    else
      diff.mkString("\n\t", "\n\t", "")
  }
}
