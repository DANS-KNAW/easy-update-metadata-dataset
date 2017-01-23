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

import java.io.StringReader

import org.scalatest.{FlatSpec, Matchers}

class InputRecordSpec extends FlatSpec with Matchers {

  "parse" should "reject invalid header" in {
    InputRecord.parse(new StringReader(
      """FEDORA_ID,STREAM_ID,XML_TAG,VALUE,REPLACEMENT
        |a,b,c
      """.stripMargin)).failed.get.getMessage shouldBe
      "header line should be: InputRecord(1,FEDORA_ID,STREAM_ID,XML_TAG,OLD_VALUE,NEW_VALUE) but was InputRecord(1,FEDORA_ID,STREAM_ID,XML_TAG,VALUE,REPLACEMENT)"
  }

  it should "reject empty fields" in {
    InputRecord.parse(new StringReader(
      """FEDORA_ID,STREAM_ID,XML_TAG,OLD_VALUE,NEW_VALUE
        |
        |a,b,
      """.stripMargin)).failed.get.getMessage shouldBe
      "incomplete line 3: a,b,"
  }

  it should "reject too few fields" in {
    InputRecord.parse(new StringReader(
      """FEDORA_ID,STREAM_ID,XML_TAG,OLD_VALUE,NEW_VALUE
        |
        |a,b
      """.stripMargin)).failed.get.getMessage shouldBe
      "incomplete line 3: a,b"
  }

  it should "reject identical values" in {
    InputRecord.parse(new StringReader(
      """FEDORA_ID,STREAM_ID,XML_TAG,OLD_VALUE,NEW_VALUE
        |
        |a,b,c,d,d
      """.stripMargin)).failed.get.getMessage shouldBe
      "old value equals new value at line 3: a,b,c,d,d"
  }

  it should "skip empty lines and process the last unterminated line" in {
    InputRecord.parse(new StringReader(
      """FEDORA_ID,STREAM_ID,XML_TAG,OLD_VALUE,NEW_VALUE
        |
        |a,b,c,d,e
        |
        |f,g,h,i,j""".stripMargin)).get shouldBe Stream(
      InputRecord(3, "a", "b", "c", "d", "e"),
      InputRecord(5, "f", "g", "h", "i", "j")
    )
  }
}
