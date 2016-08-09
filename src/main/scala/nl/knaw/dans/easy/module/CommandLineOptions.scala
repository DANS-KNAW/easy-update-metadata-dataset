package nl.knaw.dans.easy.module

import java.io.{File, PrintWriter}
import java.net.URL

import nl.knaw.dans.easy.license.CommandLineOptions._
import nl.knaw.dans.easy.license.Parameters
import org.apache.commons.configuration.PropertiesConfiguration
import org.rogach.scallop.ScallopConf
import org.slf4j.LoggerFactory

class CommandLineOptions(args: Array[String]) extends ScallopConf(args) {

  import CommandLineOptions.log

  printedName = "easy-update-metadata-dataset"
  val _________ = " " * printedName.length

  version(s"$printedName v${Version()}")
  banner(s"""
           |<Replace with one sentence describing the main task of this module>
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
