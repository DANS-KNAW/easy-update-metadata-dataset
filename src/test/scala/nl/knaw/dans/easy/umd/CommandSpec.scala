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

import java.net.{HttpURLConnection, URL}

import com.yourmediashelf.fedora.client.FedoraClient
import com.yourmediashelf.fedora.client.request.FedoraRequest
import org.scalatest._

import scala.util.{Failure, Success, Try}

class CommandSpec extends FlatSpec with Matchers {

  val url = "http://deasy.dans.knaw.nl:8080/fedora"
  val credentials = s"-f $url --fedora-username easy_webui --fedora-password easy_webui"
  val input = "src/test/resources/deasy-input.csv"

  "run" should "have no failures for EMD" in {
    assume(canConnect(Array(url)))
    val s = s"-s EMD -t accessRights $credentials $input"
    val ps = CommandLineOptions.parse(s.split(" "))
    FedoraRequest.setDefaultClient(new FedoraClient(ps.fedoraCredentials))
    Command.run(CommandLineOptions.parse(s.split(" "))) shouldBe a[Success[_]]
  }

  it should "have no failures for DC" in {
    assume(canConnect(Array(url)))
    val s = s"-s DC -t rights $credentials $input"
    Command.run(CommandLineOptions.parse(s.split(" "))) shouldBe a[Success[_]]
  }

  it should "have a failure for BLABLA" in {
    assume(canConnect(Array(url)))
    val s = s"-s BLABLA -t xyz $credentials $input"
    the[Exception] thrownBy
      Command.run(CommandLineOptions.parse(s.split(" "))).get should
      have message "failed to process: easy-dataset:2,OPEN_ACCESS"
  }

  def canConnect(urls: Array[String]): Boolean = Try {
    urls.map { url =>
      new URL(url).openConnection match {
        case connection: HttpURLConnection =>
          connection.setConnectTimeout(1000)
          connection.connect()
          connection.disconnect()
          true
        case _ => throw new Exception
      }
    }
  }.isSuccess

}
