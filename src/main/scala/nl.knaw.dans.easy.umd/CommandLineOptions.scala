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

import java.io.File
import java.net.URL

import com.yourmediashelf.fedora.client.FedoraCredentials
import org.rogach.scallop._
import org.slf4j.{Logger, LoggerFactory}

class CommandLineOptions(args: Array[String]) extends ScallopConf(args) {

  appendDefaultToDescription = true
  editBuilder(_.setHelpWidth(110))

  printedName = "easy-update-metadata-dataset"
  val description = """Batch-updates XML streams of objects in a Fedora Commons repository."""
  val synopsis = s"""$printedName <datasets.csv>"""

  version(s"$printedName v${Version()}")
  banner(
    s"""
       |  $description
       |
       |Usage:
       |
       |  $synopsis
       |
       |Options:
       |""".stripMargin)

  val doUpdate: ScallopOption[Boolean] = opt[Boolean](name = "doUpdate", noshort = true,
    descr = "Without this argument no changes are made to the repository, the default is a test mode that logs the intended changes",
    default = Some(false))

  val fedoraUrl: ScallopOption[URL] = opt[URL](name = "fedora-url", short = 'f',
    descr = "Base url for the fedora repository",
    default = Some(new URL("http://localhost:8080/fedora")))
  val fedoraUsername: ScallopOption[String] = opt[String](name = "fedora-username", noshort = true,
    descr = "Username for fedora repository, if omitted provide it on stdin")
  val fedoraPassword: ScallopOption[String] = opt[String](name = "fedora-password", noshort = true,
    descr = "Password for fedora repository, if omitted provide it on stdin")

  val inputFile: ScallopOption[File] = trailArg[File](name = "input-file", required = true,
    descr = "The CSV file (RFC4180) with required changes. The first line must be 'FEDORA_ID,STREAM_ID,XML_TAG,OLD_VALUE,NEW_VALUE', in that order. Additional columns and empty lines are ignored.")

  validateFileExists(inputFile)
  validateFileIsFile(inputFile)

  footer("")
  verify()
}

object CommandLineOptions {

  val log: Logger = LoggerFactory.getLogger(getClass)

  def parse(args: Array[String]): Parameters = {
    log.debug("Parsing command line ...")
    val opts = new CommandLineOptions(args)

    val fedoraUrl = opts.fedoraUrl()
    val fedoraUser = opts.fedoraUsername.toOption.getOrElse(ask(fedoraUrl.toString, "user name"))
    val fedoraPassword = opts.fedoraPassword.toOption.getOrElse(askPassword(fedoraUser, fedoraUrl.toString))
    val fedoraCredentials = new FedoraCredentials(fedoraUrl, fedoraUser, fedoraPassword) {
      override def toString = s"fedoraURL=${fedoraUrl.toString}, user=$fedoraUser" // prevents logging a password
    }

    // Fill Parameters with values from command line
    val params = Parameters(!opts.doUpdate(), fedoraCredentials, opts.inputFile())

    log.debug(s"Using the following settings: $params")

    params
  }

  def ask(url: String, prompt: String): String = {
    print(s"$prompt for $url: ")
    System.console().readLine()
  }

  def askPassword(user: String, url: String): String = {
    print(s"Password for $user on $url: ")
    System.console().readPassword().mkString
  }
}
