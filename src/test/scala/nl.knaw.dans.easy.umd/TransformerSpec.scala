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

import org.joda.time.{ DateTime, DateTimeUtils, DateTimeZone }
import org.scalatest.{ Inside, OptionValues }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Failure
import scala.xml.PrettyPrinter

class TransformerSpec extends AnyFlatSpec with Matchers with OptionValues with Inside {

  DateTimeZone.setDefault(DateTimeZone.forOffsetHours(1))
  DateTimeUtils.setCurrentMillisFixed(new DateTime("2016-12-09T13:52:51.089+01:00").getMillis)

  "a plain transformation" should "replace a tag with the specified old value" in {
    val inputXML = <someroot><pfx:sometag>first value</pfx:sometag><sometag>second value</sometag></someroot>
    val expectedXML = <someroot><pfx:sometag>new value</pfx:sometag><sometag>second value</sometag></someroot>

    Transformer("SOMESTREAMID", "sometag", "first value", "new value")
      .transform(inputXML).headOption.map(new PrettyPrinter(160, 2).format(_))
      .value shouldBe new PrettyPrinter(160, 2).format(expectedXML)
  }

  it should "properly process UTF8 characters" in {
    val inputXML = <someroot><pfx:sometag>Tïtel van de dataset</pfx:sometag></someroot>
    val expectedXML = <someroot><pfx:sometag>Planetoïde van issue EASY-1128</pfx:sometag></someroot>

    Transformer("SOMESTREAMID", "sometag", "Tïtel van de dataset", "Planetoïde van issue EASY-1128")
      .transform(inputXML).headOption.map(new PrettyPrinter(160, 2).format(_))
      .value shouldBe new PrettyPrinter(160, 2).format(expectedXML)
  }

  "AMD <datasetState>" should "handle initial sword submit" in {
    val inputXML =
      <damd:administrative-md version="0.1">
        <datasetState>SUBMITTED</datasetState>
        <previousState>DRAFT</previousState>
        <lastStateChange>2016-12-09T13:42:52.433+01:00</lastStateChange>
        <depositorId>user001</depositorId>
        <stateChangeDates>
          <damd:stateChangeDate>
            <fromState>DRAFT</fromState>
            <toState>SUBMITTED</toState>
            <changeDate>2016-12-09T13:42:52.434+01:00</changeDate>
          </damd:stateChangeDate>
        </stateChangeDates>
        <groupIds/>
        <damd:workflowData version="0.1">
        <assigneeId>NOT_ASSIGNED</assigneeId>
        <wfs:workflow>...</wfs:workflow>
        </damd:workflowData>
      </damd:administrative-md>

    val expectedXML =
      <damd:administrative-md version="0.1">
        <datasetState>PUBLISHED</datasetState>
        <previousState>SUBMITTED</previousState>
        <lastStateChange>2016-12-09T13:52:51.089+01:00</lastStateChange>
        <depositorId>user001</depositorId>
        <stateChangeDates>
          <damd:stateChangeDate>
            <fromState>DRAFT</fromState>
            <toState>SUBMITTED</toState>
            <changeDate>2016-12-09T13:42:52.434+01:00</changeDate>
          </damd:stateChangeDate>
          <damd:stateChangeDate>
            <fromState>SUBMITTED</fromState>
            <toState>PUBLISHED</toState>
            <changeDate>2016-12-09T13:52:51.089+01:00</changeDate>
          </damd:stateChangeDate>
        </stateChangeDates> <groupIds/> <damd:workflowData version="0.1">
          <assigneeId>NOT_ASSIGNED</assigneeId>
          <wfs:workflow>...</wfs:workflow>
        </damd:workflowData>
      </damd:administrative-md>

    Transformer("AMD", "datasetState", "SUBMITTED", "PUBLISHED")
      .transform(inputXML).headOption.map(new PrettyPrinter(160, 2).format(_))
      .value shouldBe new PrettyPrinter(160, 2).format(expectedXML)
  }

  it should "reject an actual state that doesn't equal the old state" in {
    val inputXML =
      <damd:administrative-md version="0.1">
        <datasetState>SUBMITTED</datasetState>
        <previousState>DRAFT</previousState>
        <lastStateChange>2016-12-09T13:42:52.433+01:00</lastStateChange>
        <depositorId>user001</depositorId>
        <stateChangeDates>
          <damd:stateChangeDate>
            <fromState>DRAFT</fromState>
            <toState>SUBMITTED</toState>
            <changeDate>2016-12-09T13:42:52.434+01:00</changeDate>
          </damd:stateChangeDate>
        </stateChangeDates>
        <groupIds/>
        <damd:workflowData version="0.1">
        <assigneeId>NOT_ASSIGNED</assigneeId>
        <wfs:workflow>...</wfs:workflow>
        </damd:workflowData>
      </damd:administrative-md>

    inside(Transformer.validate("AMD", "datasetState", "PUBLISHED", inputXML)) {
      case Failure(e) => e should have message "expected AMD <datasetState> [PUBLISHED] but found [SUBMITTED]."
    }
  }

  it should "reject multiple datasetState-s, whatever their values are" in {
    val inputXML =
      <damd:administrative-md version="0.1">
        <datasetState>PUBLISHED</datasetState>
        <datasetState>PUBLISHED</datasetState>
        <previousState>DRAFT</previousState>
        <lastStateChange>2016-12-09T13:42:52.433+01:00</lastStateChange>
        <depositorId>user001</depositorId>
        <stateChangeDates>
          <damd:stateChangeDate>
            <fromState>DRAFT</fromState>
            <toState>SUBMITTED</toState>
            <changeDate>2016-12-09T13:42:52.434+01:00</changeDate>
          </damd:stateChangeDate>
        </stateChangeDates>
        <groupIds/>
        <damd:workflowData version="0.1">
        <assigneeId>NOT_ASSIGNED</assigneeId>
        <wfs:workflow>...</wfs:workflow>
        </damd:workflowData>
      </damd:administrative-md>

    inside(Transformer.validate("AMD", "datasetState", "PUBLISHED", inputXML)) {
      case Failure(e) => e should have message "expected AMD <datasetState> [PUBLISHED] but found [PUBLISHEDPUBLISHED]."
    }
  }

  it should "reject an initial web-ui draft because a missing <previousState> is not implemented" in {
    val inputXML =
      <damd:administrative-md version="0.1">
        <datasetState>DRAFT</datasetState>
        <depositorId>user002</depositorId>
        <stateChangeDates/>
        <groupIds/>
        <damd:workflowData version="0.1">
          <assigneeId>NOT_ASSIGNED</assigneeId>
          <wfs:workflow>...</wfs:workflow>
        </damd:workflowData>
      </damd:administrative-md>

    inside(Transformer.validate("AMD", "datasetState", "DRAFT", inputXML)) {
      case Failure(e) => e should have message "no <previousState> in AMD."
    }
  }
}
