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

  implicit val parameters = Parameters(test = true, fedoraCredentials = null, input = null)

  "update" should "report fedora read error" in {

    expectOneLogInfo("InputRecord(easy-dataset:1,STID,TAG,old,new)")
    expectOneFedoraGetXml(Failure(new Exception("mocked message")))

    val inputRecord = InputRecord(fedoraPid = "easy-dataset:1", streamID = "STID", tag = "TAG", newValue = "new", oldValue = "old")

    val throwable = Command.update(inputRecord).failed.get
    throwable.getCause.getMessage should include("mocked message")
    throwable.getMessage shouldBe "failed to process: InputRecord(easy-dataset:1,STID,TAG,old,new), reason: mocked message"
  }

  it should "report fedora write error" in {

    expectOneLogInfo("InputRecord(easy-dataset:1,,sometag,first value,new)")
    expectOneLogInfo("old :   <sometag>first value</sometag>")
    expectOneLogInfo("new :   <sometag>new</sometag>")
    expectOneFedoraGetXml(Success(<someroot><sometag>first value</sometag> <sometag>second value</sometag></someroot>))
    expectOneFedoraUpdate(Failure(new Exception("mocked message")))

    val inputRecord = InputRecord(fedoraPid = "easy-dataset:1", streamID = "", tag = "sometag", newValue = "new", oldValue = "first value")

    val throwable = Command.update(inputRecord).failed.get
    throwable.getCause.getMessage should include("mocked message")
    throwable.getMessage shouldBe "failed to process: InputRecord(easy-dataset:1,,sometag,first value,new), reason: mocked message"
  }

  it should "report a missing previousState" in {

    expectOneLogInfo("InputRecord(easy-dataset:1,AMD,datasetState,first value,new)")
    expectOneFedoraGetXml(Success(<someroot><sometag>first value</sometag> <sometag>second value</sometag></someroot>))

    val inputRecord = InputRecord(fedoraPid = "easy-dataset:1", streamID = "AMD", tag = "datasetState", newValue = "new", oldValue = "first value")

    val throwable = Command.update(inputRecord).failed.get
    throwable.getCause.getMessage should include("previousState")
    throwable.getMessage shouldBe "failed to process: InputRecord(easy-dataset:1,AMD,datasetState,first value,new), reason: no <previousState> in AMD."
  }

  it should "preserve UTF8 characters" in {

    expectOneLogInfo("new EMD:   <title>Planetoïde van issue EASY-1128</title>")
    expectOneLogInfo("old EMD:   <title>Titel van de dataset</title>")
    expectOneLogInfo("InputRecord(easy-dataset:1,EMD,title,Titel van de dataset,Planetoïde van issue EASY-1128)")
    expectOneFedoraGetXml(Success(<someroot><title>Titel van de dataset</title> <sometag>tweeën</sometag></someroot>))
    expectOneFedoraUpdate(Success(Unit))

    // reads a CSV file with UTF8 in the new value, the file can also be applied manually as explained in its comment column
    val inputRecord = parse(new java.io.File("src/test/resources/deasy-UTF8-input.csv")).get.head

    Command.update(inputRecord) shouldBe a[Success[_]]
  }
}