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

import com.yourmediashelf.fedora.client.request.FedoraRequest
import com.yourmediashelf.fedora.client.{ FedoraClient, FedoraCredentials }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import resource._

import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, XML }

trait FedoraStreams {

  def updateDatastream(pid: String, streamId: String, content: String): Try[Unit]

  def getXml(pid: String, streamId: String): Try[Elem]
}

abstract class AbstractFedoraFedoraStreams(timeout: Long = 1000L) extends FedoraStreams with DebugEnhancedLogging {

  def updateDatastream(pid: String, streamId: String, content: String): Try[Unit] = {
    logger.info(s"updating $pid/$streamId")
    val request = FedoraClient.modifyDatastream(pid, streamId).content(content)
    executeRequest(pid, streamId, request)
  }

  def getXml(pid: String, streamId: String): Try[Elem] = Try(
    managed(FedoraClient.getDatastreamDissemination(pid, streamId).execute())
      .acquireAndGet(fedoraResponse => XML.load(fedoraResponse.getEntityInputStream))
  )

  def executeRequest[T](pid: String, streamId: String, request: FedoraRequest[T]): Try[Unit]
}

class TestFedoraStreams extends AbstractFedoraFedoraStreams {
  def executeRequest[T](pid: String, streamId: String, request: FedoraRequest[T]): Try[Unit] = {
    Success(logger.info(s"test-mode: skipping request for $pid/$streamId"))
  }
}

class FedoraFedoraStreams(timeout: Long = 1000L, fedoraCredentials: FedoraCredentials) extends AbstractFedoraFedoraStreams {
  def executeRequest[T](pid: String, streamId: String, request: FedoraRequest[T]): Try[Unit] = {
    logger.info(s"executing request for $pid/$streamId")

    managed(request.execute(new FedoraClient(fedoraCredentials)))
      .map(_.getStatus match {
        case 200 =>
          logger.info(s"saved $pid/$streamId")
          Success(())
        case status => Failure(new IllegalStateException(s"got status $status"))
      })
      .tried
      .flatten
  }
}

object FedoraStreams {

  def apply(timeout: Long = 1000L, fedoraCredentials: FedoraCredentials)(implicit parameters: Parameters): FedoraStreams = {
    if (parameters.test) new TestFedoraStreams
    else new FedoraFedoraStreams(timeout, fedoraCredentials)
  }
}
