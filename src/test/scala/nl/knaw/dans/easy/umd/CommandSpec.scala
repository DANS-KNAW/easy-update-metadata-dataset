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

import org.scalatest.{FlatSpec, Matchers}

import scala.util.{Failure, Success, Try}
import scala.xml.Elem

class CommandSpec extends FlatSpec with Matchers {

  private def expectFedoraStream(value1: Try[Elem]) = {
    new TestFedoraStreams {

      override def getXml(pid: String, streamId: String): Try[Elem] = {
        value1
      }
    }
  }

  "update" should "report fedora read error" in {

    implicit val fedoraStreams = expectFedoraStream(Failure(new Exception("mocked message")))
    implicit val parameters = Parameters(streamID = "", tag = "", test = true, fedoraCredentials = null, input = null)
    val inputRecord = InputRecord(fedoraPid = "easy-dataset:1", newValue = "new", oldValue = "old")

    val throwable = Command.update(inputRecord).failed.get
    throwable.getCause.getMessage should include("mocked message")
    throwable.getMessage shouldBe "failed to process: InputRecord(easy-dataset:1,new,old)"
  }

  it should "report fedora write error" in {

    implicit val fedoraStreams = new TestFedoraStreams {

      override def getXml(pid: String, streamId: String) =
        Success(<someroot><sometag>first value</sometag><sometag>second value</sometag></someroot>)

      override def updateDatastream(pid: String, streamId: String, content: String) =
        Failure(new Exception("mocked message"))
    }
    implicit val parameters = Parameters(streamID = "", tag = "sometag", test = true, fedoraCredentials = null, input = null)
    val inputRecord = InputRecord(fedoraPid = "easy-dataset:1", newValue = "new", oldValue = "first value")

    val throwable = Command.update(inputRecord).failed.get
    throwable.getCause.getMessage should include("mocked message")
    throwable.getMessage shouldBe "failed to process: InputRecord(easy-dataset:1,new,first value)"
  }

  it should "report a missing previousState" in {

    implicit val fedoraStreams = expectFedoraStream(Success(<someroot><sometag>first value</sometag> <sometag>second value</sometag></someroot>))
    implicit val parameters = Parameters(streamID = "AMD", tag = "datasetState", test = true, fedoraCredentials = null, input = null)
    val inputRecord = InputRecord(fedoraPid = "easy-dataset:1", newValue = "new", oldValue = "first value")


    val throwable = Command.update(inputRecord).failed.get
    throwable.getCause.getMessage should include("previousState")
    throwable.getMessage shouldBe "failed to process: InputRecord(easy-dataset:1,new,first value)"
  }

  it should "log a succeeded update" in {

    implicit val fedoraStreams = expectFedoraStream(Success(<someroot><sometag>first value</sometag> <sometag>second value</sometag></someroot>))
    implicit val parameters = Parameters(streamID = "someStream", tag = "sometag", test = true, fedoraCredentials = null, input = null)
    val inputRecord = InputRecord(fedoraPid = "easy-dataset:1", newValue = "new", oldValue = "first value")

    // tried with an implicit log parameter for the update method, otherwise as in easy-update-solr-index
    // (log.info(_: String)) expects "old someStream List(  <sometag>first value</sometag>)" once()
    // (log.info(_: String)) expects "old someStream List(  <sometag>new</sometag>)" once()
    // (log.info(_: String)) expects "test-mode: skipping request for easy-dataset:1/someStream" once()

    Command.update(inputRecord) shouldBe a[Success[_]]
  }
}
