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


import org.scalatest.{ Inside, OptionValues }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.{ Failure, Success }
import scala.xml.PrettyPrinter

class IsniTransformerSpec extends AnyFlatSpec with Matchers with OptionValues with Inside {

  "EMD <orgISNI>" should "add ISNI organizationId" in {
    val inputXML =
      <emd:easymetadata>
        <emd:title>
          <dct:alternative>BAAC Rapport 0427</dct:alternative>
        </emd:title>
        <emd:creator>
          <eas:creator>
            <eas:title>drs</eas:title>
            <eas:initials>A</eas:initials>
            <eas:surname>B</eas:surname>
            <eas:organization>BAAC</eas:organization>
            <eas:entityId eas:scheme="DAI"></eas:entityId>
          </eas:creator>
          <eas:creator>
            <eas:title>drs</eas:title>
            <eas:initials>MC</eas:initials>
            <eas:surname>R</eas:surname>
            <eas:organization>BAAC bv</eas:organization>
            <eas:entityId eas:scheme="DAI"></eas:entityId>
          </eas:creator>
        </emd:creator>
      </emd:easymetadata>

    val expectedXML =
      <emd:easymetadata>
        <emd:title>
          <dct:alternative>BAAC Rapport 0427</dct:alternative>
        </emd:title>
        <emd:creator>
          <eas:creator>
            <eas:title>drs</eas:title>
            <eas:initials>A</eas:initials>
            <eas:surname>B</eas:surname>
            <eas:organization>BAAC</eas:organization>
            <eas:entityId eas:scheme="DAI"></eas:entityId>
          </eas:creator>
          <eas:creator>
            <eas:title>drs</eas:title>
            <eas:initials>MC</eas:initials>
            <eas:surname>R</eas:surname>
            <eas:organization>BAAC bv</eas:organization>
            <eas:organizationId eas:identification-system="http://isni.org" eas:scheme="ISNI">http://isni.org/isni/0000000472370000</eas:organizationId>
            <eas:entityId eas:scheme="DAI"></eas:entityId>
          </eas:creator>
        </emd:creator>
      </emd:easymetadata>

    Transformer("EMD", "orgISNI", "BAAC bv", "0000 0004 7237 0000")
      .transform(inputXML).headOption.map(new PrettyPrinter(160, 2).format(_))
      .value shouldBe new PrettyPrinter(160, 2).format(expectedXML)
  }

  it should "reject a dataset that does NOT contain the requested organization" in {
    val inputXML =
      <emd:easymetadata>
        <emd:title>
          <dct:alternative>BAAC Rapport 0427</dct:alternative>
        </emd:title>
        <emd:creator>
          <eas:creator>
            <eas:title>drs</eas:title>
            <eas:initials>A</eas:initials>
            <eas:surname>B</eas:surname>
            <eas:organization>BAAC</eas:organization>
            <eas:entityId eas:scheme="DAI"></eas:entityId>
          </eas:creator>
          <eas:creator>
            <eas:title>drs</eas:title>
            <eas:initials>MC</eas:initials>
            <eas:surname>R</eas:surname>
            <eas:organization>BAAC bv</eas:organization>
            <eas:entityId eas:scheme="DAI"></eas:entityId>
          </eas:creator>
        </emd:creator>
      </emd:easymetadata>

    inside(Transformer.validate("EMD", "orgISNI", "BAAC 123", inputXML)) {
      case Failure(e) => e should have message "no organization with name [BAAC 123] found."
    }
  }

  it should "validate a dataset that does contain the requested organization" in {
    val inputXML =
      <emd:easymetadata>
        <emd:title>
          <dct:alternative>BAAC Rapport 0427</dct:alternative>
        </emd:title>
        <emd:creator>
          <eas:creator>
            <eas:title>drs</eas:title>
            <eas:initials>A</eas:initials>
            <eas:surname>B</eas:surname>
            <eas:organization>BAAC</eas:organization>
            <eas:entityId eas:scheme="DAI"></eas:entityId>
          </eas:creator>
          <eas:creator>
            <eas:title>drs</eas:title>
            <eas:initials>MC</eas:initials>
            <eas:surname>R</eas:surname>
            <eas:organization>BAAC bv</eas:organization>
            <eas:entityId eas:scheme="DAI"></eas:entityId>
          </eas:creator>
        </emd:creator>
      </emd:easymetadata>

    Transformer.validate("EMD", "orgISNI", "BAAC bv", inputXML) shouldBe a [Success[_]]
  }

  it should "add ROR organizationId" in {
    val inputXML =
      <emd:easymetadata>
        <emd:title>
          <dct:alternative>AMZ Publicaties 2005-12</dct:alternative>
        </emd:title>
        <emd:creator>
          <eas:creator>
            <eas:initials>J.K.</eas:initials>
            <eas:prefix>van den</eas:prefix>
            <eas:surname>Laan</eas:surname>
            <eas:organization>Andere organisatie</eas:organization>
            <eas:entityId eas:scheme="DAI"></eas:entityId>
          </eas:creator>
          <eas:creator>
            <eas:initials>J.</eas:initials>
            <eas:surname>Land</eas:surname>
            <eas:organization>Hazenberg Archeologie</eas:organization>
            <eas:entityId eas:scheme="DAI"></eas:entityId>
          </eas:creator>
        </emd:creator>
      </emd:easymetadata>

    val expectedXML =
      <emd:easymetadata>
        <emd:title>
          <dct:alternative>AMZ Publicaties 2005-12</dct:alternative>
        </emd:title>
        <emd:creator>
          <eas:creator>
            <eas:initials>J.K.</eas:initials>
            <eas:prefix>van den</eas:prefix>
            <eas:surname>Laan</eas:surname>
            <eas:organization>Andere organisatie</eas:organization>
            <eas:entityId eas:scheme="DAI"></eas:entityId>
          </eas:creator>
          <eas:creator>
            <eas:initials>J.</eas:initials>
            <eas:surname>Land</eas:surname>
            <eas:organization>Hazenberg Archeologie</eas:organization>
            <eas:organizationId eas:identification-system="https://ror.org" eas:scheme="ROR">https://ror.org/01h1amn32</eas:organizationId>
            <eas:entityId eas:scheme="DAI"></eas:entityId>
          </eas:creator>
        </emd:creator>
      </emd:easymetadata>

    Transformer("EMD", "orgROR", "Hazenberg Archeologie", "ror.org/01h1amn32")
      .transform(inputXML).headOption.map(new PrettyPrinter(160, 2).format(_))
      .value shouldBe new PrettyPrinter(160, 2).format(expectedXML)
  }
}
