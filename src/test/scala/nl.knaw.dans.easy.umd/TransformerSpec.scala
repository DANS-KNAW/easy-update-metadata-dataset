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

import org.joda.time.{ DateTime, DateTimeUtils, DateTimeZone }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ Inside, OptionValues }

import scala.util.{ Failure, Success }
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

  it should "replace a tag with the specified old value and prefix when the prefix is explicitly given" in {
    val inputXML = <someroot><pfx:sometag>first value</pfx:sometag><pfx2:sometag>second value</pfx2:sometag></someroot>
    val expectedXML = <someroot><pfx:sometag>first value</pfx:sometag><pfx2:sometag>new value</pfx2:sometag></someroot>

    Transformer("SOMESTREAMID", "pfx2:sometag", "second value", "new value")
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

  it should "delete tag when the new value is EMPTY" in {
    val inputXML = <someroot><pfx:sometag>Tïtel van de dataset</pfx:sometag><pfx:anothertag>Some content</pfx:anothertag></someroot>
    val expectedXML = <someroot><pfx:sometag>Tïtel van de dataset</pfx:sometag></someroot>

    Transformer("SOMESTREAMID", "anothertag", "Some content", "EMPTY")
      .transform(inputXML).headOption.map(new PrettyPrinter(160, 2).format(_))
      .value shouldBe new PrettyPrinter(160, 2).format(expectedXML)
  }

  it should "delete tag with a specific prefix when the new value is EMPTY and prefix is given" in {
    val inputXML = <someroot><pfx:sometag>Tïtel van de dataset</pfx:sometag><pfx:anothertag>Some content</pfx:anothertag><pfx2:anothertag>Some content</pfx2:anothertag></someroot>
    val expectedXML = <someroot><pfx:sometag>Tïtel van de dataset</pfx:sometag><pfx:anothertag>Some content</pfx:anothertag></someroot>

    Transformer("SOMESTREAMID", "pfx2:anothertag", "Some content", "EMPTY")
      .transform(inputXML).headOption.map(new PrettyPrinter(160, 2).format(_))
      .value shouldBe new PrettyPrinter(160, 2).format(expectedXML)
  }

  it should "add a new tag in a given parent element when the old value is EMPTY" in {
    val inputXML = <someroot><somegroup><pfx:sometag>Tïtel van de dataset</pfx:sometag></somegroup></someroot>
    val expectedXML = <someroot><somegroup><pfx:sometag>Tïtel van de dataset</pfx:sometag><pfx:newtag>Something new</pfx:newtag></somegroup></someroot>

    Transformer("SOMESTREAMID", "somegroup", "EMPTY", "<pfx:newtag>Something new</pfx:newtag>")
      .transform(inputXML).headOption.map(new PrettyPrinter(160, 2).format(_))
      .value shouldBe new PrettyPrinter(160, 2).format(expectedXML)
  }

  it should "not add a new tag in a given parent element if the new tag exists already" in {
    val inputXML = <someroot><somegroup><pfx:sometag>Tïtel van de dataset</pfx:sometag></somegroup></someroot>
    val expectedXML = <someroot><somegroup><pfx:sometag>Tïtel van de dataset</pfx:sometag><pfx:newtag>Something new</pfx:newtag></somegroup></someroot>

    val newXml = Transformer("SOMESTREAMID", "somegroup", "EMPTY", "<pfx:newtag>Something new</pfx:newtag>").transform(inputXML)
    Transformer("SOMESTREAMID", "somegroup", "EMPTY", "<pfx:newtag>Something new</pfx:newtag>").transform(newXml)
      .headOption.map(new PrettyPrinter(160, 2).format(_))
      .value shouldBe new PrettyPrinter(160, 2).format(expectedXML)
  }

  it should "add in EMD a new license tag in a given parent element, before rightsHolder tag, when the old value is EMPTY" in {
    val inputXML = <someroot>
      <emd:rights>
        <dct:accessRights eas:schemeId="common.dcterms.accessrights">OPEN_ACCESS</dct:accessRights>
        <dct:rightsHolder>BAAC</dct:rightsHolder>
      </emd:rights>
    </someroot>
    val expectedXML = <someroot>
      <emd:rights>
        <dct:accessRights eas:schemeId="common.dcterms.accessrights">OPEN_ACCESS</dct:accessRights>
        <dct:license>http://creativecommons.org/licenses/by/4.0</dct:license>
        <dct:rightsHolder>BAAC</dct:rightsHolder>
      </emd:rights>
    </someroot>

    Transformer("EMD", "rights", "EMPTY", "<dct:license>http://creativecommons.org/licenses/by/4.0</dct:license>")
      .transform(inputXML).headOption.map(new PrettyPrinter(160, 2).format(_))
      .value shouldBe new PrettyPrinter(160, 2).format(expectedXML)
  }

  it should "add in EMD a new license tag as last in emd:rights when there is no rightsHolder tag" in {
    val inputXML = <someroot>
      <emd:rights>
        <dct:accessRights eas:schemeId="common.dcterms.accessrights">OPEN_ACCESS</dct:accessRights>
      </emd:rights>
    </someroot>
    val expectedXML = <someroot>
      <emd:rights>
        <dct:accessRights eas:schemeId="common.dcterms.accessrights">OPEN_ACCESS</dct:accessRights>
        <dct:license>http://creativecommons.org/licenses/by/4.0</dct:license>
      </emd:rights>
    </someroot>

    Transformer("EMD", "rights", "EMPTY", "<dct:license>http://creativecommons.org/licenses/by/4.0</dct:license>")
      .transform(inputXML).headOption.map(new PrettyPrinter(160, 2).format(_))
      .value shouldBe new PrettyPrinter(160, 2).format(expectedXML)
  }

  it should "add in EMD a new tag under a specific prefix, when it is given" in {
    val inputXML = <someroot>
      <emd:contributor>
        <eas:contributor>
          <eas:organization>dans</eas:organization>
          <eas:entityId eas:scheme="DAI"></eas:entityId>
        </eas:contributor>
      </emd:contributor>
    </someroot>
    val expectedXML = <someroot>
      <emd:contributor>
        <eas:contributor>
          <eas:organization>dans</eas:organization>
          <eas:entityId eas:scheme="DAI"></eas:entityId>
        </eas:contributor>
        <eas:contributor>
          <eas:organization>Organization</eas:organization>
          <eas:role eas:scheme="DATACITE">RightsHolder</eas:role>
        </eas:contributor>
      </emd:contributor>
    </someroot>

    Transformer("EMD", "emd:contributor", "EMPTY", "<eas:contributor><eas:organization>Organization</eas:organization><eas:role eas:scheme=\"DATACITE\">RightsHolder</eas:role></eas:contributor>")
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

  "AMD <datasetState>" should "set maintenance on immediately published datasets" in {
    val inputXML =
      <damd:administrative-md version="0.1">
        <datasetState>PUBLISHED</datasetState>
        <depositorId>user001</depositorId>
        <stateChangeDates/>
      </damd:administrative-md>

    val expectedXML =
      <damd:administrative-md version="0.1">
        <datasetState>MAINTENANCE</datasetState>
        <previousState>PUBLISHED</previousState>
        <lastStateChange>2016-12-09T13:52:51.089+01:00</lastStateChange>
        <depositorId>user001</depositorId>
        <stateChangeDates>
          <damd:stateChangeDate>
            <fromState>PUBLISHED</fromState>
            <toState>MAINTENANCE</toState>
            <changeDate>2016-12-09T13:52:51.089+01:00</changeDate>
          </damd:stateChangeDate>
        </stateChangeDates>
      </damd:administrative-md>

    Transformer.validate("AMD", "datasetState", "PUBLISHED", inputXML) shouldBe a[Success[_]]
    Transformer("AMD", "datasetState", "PUBLISHED", "MAINTENANCE", isFirstDatesetStateChange = true)
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

  it should "accept an initial web-ui draft because a missing <previousState> is now implemented" in {
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

    Transformer.validate("AMD", "datasetState", "DRAFT", inputXML) shouldBe a[Success[_]]
  }
}
