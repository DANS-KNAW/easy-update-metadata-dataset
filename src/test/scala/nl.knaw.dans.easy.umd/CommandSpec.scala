/*
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

import java.io.File

import org.apache.commons.io.FileUtils
import org.scalamock.scalatest.MockFactory
import org.scalatest.{ Inside, OneInstancePerTest }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.{ Failure, Success, Try }
import scala.xml.Elem

class CommandSpec extends AnyFlatSpec with Matchers with Inside with MockFactory with OneInstancePerTest {

  private implicit val fedoraMock: FedoraStreams = mock[FedoraStreams]

  private def expectOneFedoraGetXml(triedElem: Try[Elem]) = fedoraMock.getXml _ expects(*, *) once() returning triedElem

  private def expectFedoraUpdates(returnValue: Try[Unit], times: Int) = fedoraMock.updateDatastream _ expects(*, *, *) returning returnValue repeat times

  private def expectUtf8Record(record: Int, fedoraID: Int, stream: String) = {
    expectOneFedoraGetXml(Success(
      <someroot>
        <title>Titel van de dataset</title> <sometag>tweeën</sometag>
      </someroot>
    ))
  }

  "update" should "report fedora read error" in {
    expectOneFedoraGetXml(Failure(new Exception("mocked message")))

    val inputRecord = InputRecord(fedoraID = "easy-dataset:1", streamID = "STID", xmlTag = "TAG", newValue = "new", oldValue = "old")
    implicit val parameters: Parameters = Parameters(test = true, fedoraCredentials = null, input = null)

    inside(UpdateMetadataDataset.update(fedoraMock)(inputRecord)) {
      case Failure(e) =>
        e should have message "failed to process: InputRecord(1,easy-dataset:1,STID,TAG,old,new), reason: mocked message"
        e.getCause.getMessage should include("mocked message")
    }
  }

  it should "report fedora write error" in {
    expectOneFedoraGetXml(Success(<someroot><sometag>first value</sometag> <sometag>second value</sometag></someroot>))
    expectFedoraUpdates(returnValue = Failure(new Exception("mocked message")), times = 1)

    val inputRecord = InputRecord(fedoraID = "easy-dataset:1", streamID = "", xmlTag = "sometag", newValue = "new", oldValue = "first value")
    implicit val parameters: Parameters = Parameters(test = true, fedoraCredentials = null, input = null)

    inside(UpdateMetadataDataset.update(fedoraMock)(inputRecord)) {
      case Failure(e) =>
        e should have message "failed to process: InputRecord(1,easy-dataset:1,,sometag,first value,new), reason: mocked message"
        e.getCause.getMessage should include("mocked message")
    }
  }

  it should "report an inconsistent old AMD <datasetState> value" in {
    expectOneFedoraGetXml(Success(<someroot><datasetState>first value</datasetState> <previousState>second value</previousState></someroot>))

    val inputRecord = InputRecord(fedoraID = "easy-dataset:1", streamID = "AMD", xmlTag = "datasetState", newValue = "new", oldValue = "other value")
    implicit val parameters: Parameters = Parameters(test = true, fedoraCredentials = null, input = null)

    inside(UpdateMetadataDataset.update(fedoraMock)(inputRecord)) {
      case Failure(e) =>
        e should have message "failed to process: InputRecord(1,easy-dataset:1,AMD,datasetState,other value,new)" +
          ", reason: expected AMD <datasetState> [other value] but found [first value]."
    }
  }

  "testFriendlyRun" should "preserve UTF8 characters of CSV" in {
    expectUtf8Record(record = 2, fedoraID = 1, stream = "EMD")
    expectUtf8Record(record = 3, fedoraID = 1, stream = "DC")
    expectUtf8Record(record = 4, fedoraID = 2, stream = "EMD")
    expectUtf8Record(record = 5, fedoraID = 2, stream = "DC")
    expectFedoraUpdates(returnValue = Success(()), times = 4)

    // CSV file with UTF8 in the new value, the file can also be applied manually as explained in its comment column
    val file = new File("src/test/resources/deasy-UTF8-input.csv")
    implicit val ps: Parameters = Parameters(test = true, fedoraCredentials = null, input = file)
    UpdateMetadataDataset.testFriendlyRun(fedoraMock).acquireAndGet(identity) shouldBe a[Success[_]]
  }

  it should "continue after reporting a problem" in {
    expectUtf8Record(record = 2, fedoraID = 1, stream = "EMD")
    expectOneFedoraGetXml(Failure(new Exception("mocked message")))
    expectOneFedoraGetXml(Failure(new Exception("mocked message")))
    expectUtf8Record(record = 5, fedoraID = 2, stream = "DC")
    expectFedoraUpdates(returnValue = Success(()), times = 2)

    // CSV file with UTF8 in the new value, the file can also be applied manually as explained in its comment column
    val file = new File("src/test/resources/deasy-UTF8-input.csv")
    implicit val ps: Parameters = Parameters(test = true, fedoraCredentials = null, input = file)
    //note that errors are also logged by UpdateMetadataDataset.update
    UpdateMetadataDataset.testFriendlyRun(fedoraMock).acquireAndGet(identity)
      .toEither.left.get.getMessage.replace("occurred: ","occurred:") shouldBe
     """2 exceptions occurred:
       |--- START OF EXCEPTION LIST ---
       |(0) failed to process: InputRecord(3,easy-dataset:1,DC,title,Titel van de dataset,Planetoïde van issue EASY-1128), reason: mocked message
       |(1) failed to process: InputRecord(4,easy-dataset:2,EMD,title,Titel van de dataset,Planetoïde van issue EASY-1128), reason: mocked message
       |--- END OF EXCEPTION LIST ---.""".stripMargin
  }

  it should "reject CSV when UTF-8 decoding finds invalid characters" in {
    val file = new File("src/test/resources/macroman.txt")
    implicit val ps: Parameters = Parameters(test = true, fedoraCredentials = null, input = file)

    inside(UpdateMetadataDataset.testFriendlyRun(fedoraMock).map(identity).tried.flatten) {
      case Failure(e) => e should have message "MalformedInputException reading next record: java.nio.charset.MalformedInputException: Input length = 1"
    }
  }

  "textReader" should "not try to guess the encoding" in {
    // saved with Mac's text editor with "Western (Mac OS Roman)"
    val file = new File("src/test/resources/macroman.txt")
    FileUtils.readFileToString(file, "x-MacRoman") shouldBe "ÈÀŒØ"

    // guessed by tika library in commit a3dae76
    FileUtils.readFileToString(file, "KOI8-R") shouldBe "Икн╞"

    // guessed by system command: file --mime-encoding src/test/resources/*
    FileUtils.readFileToString(file, "iso-8859-1") shouldBe "éËÎ¯"

    FileUtils.readFileToString(file, "UTF-8") shouldBe "��ί"
  }
}
