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
import nl.knaw.dans.easy.umd.StreamUpdater.log
import org.slf4j.LoggerFactory
import resource._

import scala.util.{Failure, Success, Try}

trait StreamUpdater {

  def updateDatastream(pid: String, streamId: String, content: String): Try[Unit]
}

abstract class AbstractFedoraStreamUpdater(timeout: Long = 1000L) extends StreamUpdater {

  def updateDatastream(pid: String, streamId: String, content: String) = {
    log.info(s"updating $pid/$streamId")
    val request = FedoraClient.modifyDatastream(pid, streamId).content(content)
    executeRequest(pid, streamId, request)
  }

  def executeRequest(pid: String, streamId: String, request: FedoraRequest[_]): Try[Unit]
}

class TestStreamUpdater extends AbstractFedoraStreamUpdater {
  def executeRequest(pid: String, streamId: String, request: FedoraRequest[_]) =
    Success(log.info(s"test-mode: skipping request for $pid/$streamId"))
}

class FedoraStreamUpdater(timeout: Long = 1000L) extends AbstractFedoraStreamUpdater {
  def executeRequest(pid: String, streamId: String, request: FedoraRequest[_]) = {
    log.info(s"executing request for $pid/$streamId")
    managed(request.execute()).acquireAndGet(_.getStatus match {
      case 200 => log.info(s"saved $pid/$streamId")
        Success(Unit)
      case status =>
        Failure(new IllegalStateException(s"got status $status"))
    }) // TODO catch and flatmap exception thrown by acquireAndGet or
    // https://github.com/DANS-KNAW/easy-app/blob/419d5b06f/tool/task-change-set-to-openaccess/src/main/scala/nl/knaw/dans/easy/task/StreamUpdater.scala#L21-L35
  }
}

object StreamUpdater {

  val log = LoggerFactory.getLogger(getClass)

  def apply(timeout: Long = 1000L)(implicit parameters: Parameters): StreamUpdater =
    if (parameters.test) new TestStreamUpdater
    else new FedoraStreamUpdater(timeout)
}
