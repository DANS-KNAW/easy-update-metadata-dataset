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

import com.yourmediashelf.fedora.client.FedoraClient
import com.yourmediashelf.fedora.client.request.FedoraRequest
import nl.knaw.dans.easy.umd.InputRecord.parse
import nl.knaw.dans.easy.umd.{CommandLineOptions => cmd}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}
import scala.xml.PrettyPrinter

object Command {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    log.debug("Starting command line interface")
    val ps = cmd.parse(args)
    FedoraRequest.setDefaultClient(new FedoraClient(ps.fedoraCredentials))
    run(ps) match {
      case Success((count)) => log.info(s"Changed $count ${ps.streamID} streams.")
      case Failure(e) => log.error("failed", e)
    }
  }

  def run(implicit ps: Parameters): Try[Unit] = for {
    records <- parse(ps.input)
    _ <- records.map(update).find(_.isFailure).getOrElse(Success(())) // fail fast, if an error occurs, stop updating the rest of the stream!
  } yield records.size

  def update(record: InputRecord)(implicit ps: Parameters): Try[Unit] = {
    log.info(s"${record.fedoraPid}, ${record.newValue}")
    val fedora = FedoraStreams()
    for {
      oldXML <- fedora.getXml(record.fedoraPid, ps.streamID)
      transformer <- Transformer(ps.streamID, ps.tag, oldXML, record.newValue)
      newXML = transformer.transform(oldXML)
      oldLines = new PrettyPrinter(160, 2).format(oldXML).lines.toList
      newLines = new PrettyPrinter(160, 2).format(newXML.head).lines.toList
      _ = log.info(s"old ${ps.streamID} ${oldLines.diff(newLines)}")
      _ = log.info(s"new ${ps.streamID} ${newLines.diff(oldLines)}")
      _ <- if (oldXML != newXML) fedora.updateDatastream(record.fedoraPid, ps.streamID, newXML.toString()) else Success(())
    } yield ()
  }.recoverWith { case e =>
    Failure(new Exception(s"failed to process: ${record.fedoraPid},${record.newValue}", e))
  }
}
