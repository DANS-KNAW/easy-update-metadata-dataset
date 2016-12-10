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

import org.joda.time.{DateTime, DateTimeUtils}
import org.scalatest.{FlatSpec, Matchers}

import scala.xml.PrettyPrinter

class TransformerSpec extends FlatSpec with Matchers {

  DateTimeUtils.setCurrentMillisFixed(new DateTime("2016-12-09T13:52:51.089+01:00").getMillis)

  "a plain transformation" should "replace all occurrences of a tag" in {

    val inputXML = <someroot><sometag>first value</sometag><sometag>second value</sometag></someroot>
    val expectedXML = <someroot><sometag>new value</sometag><sometag>new value</sometag></someroot>
    new PrettyPrinter(160, 2).format(
      Transformer("SOMESTREAMID", "sometag", inputXML, "new value")
        .get.transform(inputXML).head
    ) shouldBe new PrettyPrinter(160, 2).format(expectedXML)
  }

  "AMD datasetState" should "deal with initial sword submit" in {

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

    new PrettyPrinter(160, 2).format(
      Transformer("AMD", "datasetState", inputXML, "PUBLISHED")
        .get.transform(inputXML).head
    ) shouldBe new PrettyPrinter(160, 2).format(expectedXML)
  }

  it should "fail to deal deal with initial web-ui draft" in {

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

    val expectedXML =

      <damd:administrative-md version="0.1">
        <datasetState>SUBMITTED</datasetState>
        <previousState>DRAFT</previousState>
        <depositorId>user002</depositorId>
        <stateChangeDates>
          <damd:stateChangeDate/>
          <damd:stateChangeDate>
            <fromState>DRAFT</fromState>
            <toState>SUBMITTED</toState>
            <changeDate>2016-11-11T15:36:40.641+01:00</changeDate>
          </damd:stateChangeDate>
        </stateChangeDates>
        <damd:workflowData version="0.1">
          <assigneeId>NOT_ASSIGNED</assigneeId>
          <wfs:workflow>...</wfs:workflow>
        </damd:workflowData>
      </damd:administrative-md>

    Transformer("AMD", "datasetState", inputXML, "SUBMITTED")
      .failed.get.getMessage should include("previousState")
  }
}
