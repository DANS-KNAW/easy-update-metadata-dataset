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

import nl.knaw.dans.easy.umd.InputRecord.parse
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Inside, Matchers, OneInstancePerTest}
import org.slf4j.Logger

import scala.util.{Failure, Success, Try}
import scala.xml.Elem

class CommandSpec extends FlatSpec
  with Matchers
  with Inside
  with MockFactory
  with OneInstancePerTest  {

  private implicit val logMock = mock[Logger]
  private def expectOneLogInfo(message: String) = (logMock.info(_: String)) expects message once()

  private implicit val fedoraMock = mock[FedoraStreams]
  private def expectOneFedoraGetXml(result: Try[Elem]) = fedoraMock.getXml _ expects(*, *) once() returning result
  private def expectOneFedoraUpdate(failure: Try[Unit]) = fedoraMock.updateDatastream _ expects(*, *, *) once() returning failure

  "update" should "report fedora read error" in {

    expectOneLogInfo("InputRecord(easy-dataset:1,new,old)")
    expectOneFedoraGetXml(Failure(new Exception("mocked message")))

    val inputRecord = InputRecord(fedoraPid = "easy-dataset:1", newValue = "new", oldValue = "old")
    implicit val parameters = Parameters(streamID = "", tag = "", test = true, fedoraCredentials = null, input = null)

    val throwable = Command.update(inputRecord).failed.get
    throwable.getCause.getMessage should include("mocked message")
    throwable.getMessage shouldBe "failed to process: InputRecord(easy-dataset:1,new,old), reason: mocked message"
  }

  it should "report fedora write error" in {

    expectOneLogInfo("InputRecord(easy-dataset:1,new,first value)")
    expectOneLogInfo("old :   <sometag>first value</sometag>")
    expectOneLogInfo("new :   <sometag>new</sometag>")
    expectOneFedoraGetXml(Success(<someroot><sometag>first value</sometag> <sometag>second value</sometag></someroot>))
    expectOneFedoraUpdate(Failure(new Exception("mocked message")))

    val inputRecord = InputRecord(fedoraPid = "easy-dataset:1", newValue = "new", oldValue = "first value")
    implicit val parameters = Parameters(streamID = "", tag = "sometag", test = true, fedoraCredentials = null, input = null)

    val throwable = Command.update(inputRecord).failed.get
    throwable.getCause.getMessage should include("mocked message")
    throwable.getMessage shouldBe "failed to process: InputRecord(easy-dataset:1,new,first value), reason: mocked message"
  }

  it should "report a missing previousState" in {

    expectOneLogInfo("InputRecord(easy-dataset:1,new,first value)")
    expectOneFedoraGetXml(Success(<someroot><sometag>first value</sometag> <sometag>second value</sometag></someroot>))

    val inputRecord = InputRecord(fedoraPid = "easy-dataset:1", newValue = "new", oldValue = "first value")
    implicit val parameters = Parameters(streamID = "AMD", tag = "datasetState", test = true, fedoraCredentials = null, input = null)

    val throwable = Command.update(inputRecord).failed.get
    throwable.getCause.getMessage should include("previousState")
    throwable.getMessage shouldBe "failed to process: InputRecord(easy-dataset:1,new,first value), reason: no <previousState> in AMD."
  }

  it should "preserve UTF8 characters" in {

    expectOneLogInfo("new someStream:   <sometag>Planetoïde van issue EASY-1128</sometag>")
    expectOneLogInfo("old someStream:   <sometag>Titel van de dataset</sometag>")
    expectOneLogInfo("InputRecord(easy-dataset:1,Planetoïde van issue EASY-1128,Titel van de dataset)")
    expectOneFedoraGetXml(Success(<someroot><sometag>Titel van de dataset</sometag> <sometag>tweeën</sometag></someroot>))
    expectOneFedoraUpdate(Success(Unit))

    // reads a CSV file with UTF8 in the new value, the file can also be applied manually as explained in its comment column
    val inputRecord = parse(new java.io.File("src/test/resources/deasy-UTF8-input.csv")).get.head
    implicit val parameters = Parameters(streamID = "someStream", tag = "sometag", test = true, fedoraCredentials = null, input = null)

    Command.update(inputRecord) shouldBe a[Success[_]]
  }
}