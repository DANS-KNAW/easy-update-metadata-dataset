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

import java.io.{File, FileInputStream}

import com.yourmediashelf.fedora.client.FedoraClient
import com.yourmediashelf.fedora.client.request.FedoraRequest
import nl.knaw.dans.easy.umd.InputRecord.parse
import nl.knaw.dans.easy.umd.{CommandLineOptions => cmd}
import org.apache.tika.detect.AutoDetectReader
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, Node, PrettyPrinter}

object Command {
  implicit val log: Logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    val ps = cmd.parse(args)
    log.info(s"Started $ps")
    run(ps) match {
      case Success(_) => log.info(s"Finished $ps")
      case Failure(e) => log.error(s"Failed $ps", e)
    }
  }

  def run(implicit ps: Parameters): Try[Unit] = {
    FedoraRequest.setDefaultClient(new FedoraClient(ps.fedoraCredentials))
    implicit val fedora = FedoraStreams()
    testFriendlyRun
  }

  def testFriendlyRun(implicit ps: Parameters, fedora: FedoraStreams, log: Logger): Try[Unit] = for {
    reader <- textReader(ps.input)
    _ <- assertUTF8(ps.input, reader)
    records <- parse(reader)
    _ <- failFast(records.map(update))
  } yield ()

  // if an error occurs, stop updating the rest of the stream!
  private def failFast(streamOfTries: Stream[Try[Unit]]) = streamOfTries.find(_.isFailure).getOrElse(Success(Unit))

  def textReader(file: File): Try[AutoDetectReader] = Try {
    new AutoDetectReader(new FileInputStream(file))
  }

  private def assertUTF8(file: File, reader: AutoDetectReader) =
    if (reader.getCharset.toString == "UTF-8")
      Success(Unit)
    else Failure(new Exception(s"encoding of $file is not UTF-8 but ${reader.getCharset}"))

  def update(record: InputRecord)
            (implicit ps: Parameters, fedora: FedoraStreams, log: Logger): Try[Unit] = {
    log.info(record.toString)
    for {
      oldXML <- fedora.getXml(record.fedoraID, record.streamID)
      _ <- Transformer.validate(record.streamID, record.xmlTag, record.oldValue, oldXML)
      transformer = Transformer(record.streamID, record.xmlTag, record.oldValue, record.newValue)
      newXML = transformer.transform(oldXML)
      _ <- reportChanges(record, oldXML, newXML)
      _ <- fedora.updateDatastream(record.fedoraID, record.streamID, newXML.toString())
    } yield ()
  }.recoverWith { case e =>
    Failure(new Exception(s"failed to process: $record, reason: ${e.getMessage}", e))
  }

  private def reportChanges(record: InputRecord, oldXML: Elem, newXML: Seq[Node])
                   (implicit log: Logger): Try[Unit] = {
    val oldLines = new PrettyPrinter(160, 2).format(oldXML).lines.toList
    val newLines = new PrettyPrinter(160, 2).format(newXML.head).lines.toList
    if (oldXML == newXML)
      Failure(new Exception(s"could not find ${record.streamID} <${record.xmlTag}>${record.oldValue}</${record.xmlTag}>"))
    else {
      log.info(s"old ${record.streamID}: ${compare(oldLines, newLines)}")
      log.info(s"new ${record.streamID}: ${compare(newLines, oldLines)}")
      Success(Unit)
    }
  }

  /** @return lines of xs not in ys */
  private def compare(xs: List[String], ys: List[String]) = {
    val diff = xs.diff(ys)
    if (diff.size <= 1)
      diff.mkString("", "", "")
    else
      diff.mkString("\n\t", "\n\t", "")
  }
}
