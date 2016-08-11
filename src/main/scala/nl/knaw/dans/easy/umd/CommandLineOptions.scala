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
import nl.knaw.dans.easy.umd.CommandLineOptions.log
import org.rogach.scallop._
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

class CommandLineOptions(args: Array[String] = "-ss -tt -f http:// src/test/resources/deasy-input.csv".split(" ")) extends ScallopConf(args) {

  appendDefaultToDescription = true
  editBuilder(_.setHelpWidth(110))

  printedName = "easy-update-metadata-dataset"
  val _________ = " " * printedName.length

  version(s"$printedName v${Version()}")
  banner(
    s"""
       |Batch-updates metadata streams in a Fedora Commons repository
       |
       |Usage:
       |
       |$printedName --stream-id [EMD|DC|...] --tag [accessRights|rights|...] <datasets.csv>
       |
       |Options:
       |""".stripMargin)

  private val shouldBeFile = singleArgConverter(value =>
    new File(value) match {
      case f if f.isFile => f
      case _ => throw createExecption(s"'$value' is not a file")
    }
  )
  private val shouldBeDir = singleArgConverter(value =>
    new File(value) match {
      case f if f.isDirectory => f
      case _ => throw createExecption(s"'$value' is not a directory")
    }
  )
  private val shouldBeUrl = singleArgConverter(value =>
    Try {
      new URL(value)
    } match {
      case Success(url) => value
      case Failure(e) => throw createExecption(s"'$value' is not a valid url: ${e.getMessage}")
    }
  )

  private def createExecption(msg: String) = {
    log.error(msg)
    new IllegalArgumentException(msg)
  }


  val doUpdate = opt[Boolean](name = "doUpdate", noshort = true,
    descr = "Without this argument no changes are made to the repository, the default is a test mode that logs the intended changes",
    default = Some(false))

  val streamID = opt[String](name = "stream-id", short = 's', required = true, descr = "id of fedoara stream to update")
  val tag = opt[String](name = "tag", short = 't', required = true, descr = "xml tag to change")

  val fedoraUrl = opt[String](name = "fedora-url", short = 'f',
    descr = "Base url for the fedora repository",
    default = Some("http://localhost:8080/fedora"))(shouldBeUrl)
  val fedoraUsername = opt[String](name = "fedora-username", noshort = true,
    descr = "Username for fedora repository, if omitted provide it on stdin")
  val fedoraPassword = opt[String](name = "fedora-password", noshort = true,
    descr = "Password for fedora repository, if omitted provide it on stdin")

  val inputFile = trailArg[File](name = "input-file", required = true, descr = "The CSV file with required changes. Columns: fedoraID, newValue. First line is assumed to be a header.")(shouldBeFile)

  footer("")
  verify()
}

object CommandLineOptions {

  val log = LoggerFactory.getLogger(getClass)

  def parse(args: Array[String]): Parameters = {
    /*
        log.debug("Loading application properties ...")
        val homeDir = new File(System.getProperty("app.home"))
        val props = {
          val ps = new PropertiesConfiguration()
          ps.setDelimiterParsingDisabled(true)
          ps.load(new File(homeDir, "cfg/application.properties"))

          ps
        }
    */

    log.debug("Parsing command line ...")
    val opts = new CommandLineOptions(args)

    val fedoraUrl = new URL(opts.fedoraUrl())

    // TODO try properties before asking?
    val fedoraUser = opts.fedoraUsername.get.getOrElse(ask(fedoraUrl.toString, "user name"))
    val fedoraPassword = opts.fedoraPassword.get.getOrElse(askPassword(fedoraUser, fedoraUrl.toString))
    val fedoraCredentials = new FedoraCredentials(fedoraUrl, fedoraUser, fedoraPassword) {
      override def toString = s"fedoraURL=${fedoraUrl.toString}, user=$fedoraUser" // prevents logging a password
    }

    // Fill Parameters with values from command line
    val params = Parameters(opts.streamID(), opts.tag(), !opts.doUpdate(), fedoraCredentials, opts.inputFile())

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
