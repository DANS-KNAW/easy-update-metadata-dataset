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

import com.yourmediashelf.fedora.client.FedoraClient
import com.yourmediashelf.fedora.client.request.FedoraRequest
import nl.knaw.dans.easy.umd.{CommandLineOptions => cmd}
import org.apache.commons.csv.{CSVFormat, CSVParser}
import org.apache.commons.io.Charsets
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.util.{Failure, Success, Try}
import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Elem, Node, Text}

object Command {
  val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    log.debug("Starting command line interface")
    val ps = cmd.parse(args)
    FedoraRequest.setDefaultClient(new FedoraClient(ps.fedoraCredentials))
    run(ps) match {
      case s: Success[List[Any]] if s.get.nonEmpty => () // logged by run
      case f: Failure[_] => log.error(s"failed", f.exception)
      case s: Success[_] => log.info("success")
    }
  }

  def run(implicit ps: Parameters): Try[List[Any]] = {
    for {
      list <- parse(ps.input)
      _ = log.info(s"parsed ${list.length} records")
      failures = list.map(record => (record, update(record))).filter(_._2.isFailure).map(reportError)
      _ = log.info(s"processed ${list.length} records with ${failures.length} failures")
    } yield failures
  }

  def reportError(tuple: (InputRecord, Try[Unit])): Any = {
    val (record, result) = tuple
    val throwable = result.failed.get
    log.error(s"failed to process ${record.fedoraPid} ${record.newValue}", throwable)
  }

  def update(record: InputRecord)(implicit ps: Parameters): Try[Unit] = {
    log.info(s"${record.fedoraPid}, ${record.newValue}")
    for {
      oldXML <- FedoraStreams().getXml(record.fedoraPid, ps.streamID)
      newXML = transformer(ps.tag, record.newValue).transform(oldXML)
      oldLines = oldXML.toString().lines.toList
      newLines = newXML.toString().lines.toList
      _ = log.info(s"old ${ps.streamID} ${oldLines.diff(newLines)}")
      _ = log.info(s"new ${ps.streamID} ${newLines.diff(oldLines)}")
      _ <- if (oldXML != newXML) FedoraStreams().updateDatastream(record.fedoraPid, ps.streamID, newXML.toString()) else Success(())
    } yield ()
  }

  def parse(file: File): Try[List[InputRecord]] = Try {
    CSVParser
      .parse(file, Charsets.UTF_8, CSVFormat.RFC4180)
      .filter(_.nonEmpty).drop(1).toList.map(rec => InputRecord(rec))
  }

  def transformer(label: String, newValue: String) = {
    new RuleTransformer(new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case Elem(prefix, `label`, attribs, scope, children) =>
          Elem(prefix, label, attribs, scope, false, Text(newValue))
        case other => other
      }
    })
  }
}
