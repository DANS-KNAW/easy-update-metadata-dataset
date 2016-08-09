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

import nl.knaw.dans.easy.umd.{Parameters, Version}
import org.apache.commons.configuration.PropertiesConfiguration
import org.rogach.scallop.ScallopConf
import org.slf4j.LoggerFactory

class CommandLineOptions(args: Array[String]) extends ScallopConf(args) {

  appendDefaultToDescription = true
  editBuilder(_.setHelpWidth(110))

  printedName = "easy-update-metadata-dataset"
  val _________ = " " * printedName.length

  version(s"$printedName v${Version()}")
  banner(s"""
           |Batch-updates metadata streams in a Fedora Commons repository
           |
           |Usage:
           |
           |$printedName <synopsis of command line parameters>
           |${_________} <...possibly continued here>
           |
           |Options:
           |""".stripMargin)
  //val url = opt[String]("someOption", noshort = true, descr = "Description of the option", default = Some("Default value"))
  footer("")
}

object CommandLineOptions {

  val log = LoggerFactory.getLogger(getClass)

  def parse(args: Array[String]): Parameters = {
    log.debug("Loading application properties ...")
    val homeDir = new File(System.getProperty("app.home"))
    val props = {
      val ps = new PropertiesConfiguration()
      ps.setDelimiterParsingDisabled(true)
      ps.load(new File(homeDir, "cfg/application.properties"))

      ps
    }

    log.debug("Parsing command line ...")
    val opts = new CommandLineOptions(args)

    // Fill Parameters with values from command line
    val params = Parameters()

    log.debug(s"Using the following settings: $params")

    params
  }
}
