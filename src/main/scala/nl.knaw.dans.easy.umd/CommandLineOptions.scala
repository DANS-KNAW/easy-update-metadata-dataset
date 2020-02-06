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

import java.io.File

import org.rogach.scallop._

class CommandLineOptions(args: Array[String], configuration: Configuration) extends ScallopConf(args) {

  appendDefaultToDescription = true
  editBuilder(_.setHelpWidth(110))

  printedName = "easy-update-metadata-dataset"
  version(s"$printedName v${ configuration.version }")
  val description = "Batch-updates XML streams of objects in a Fedora Commons repository."
  val synopsis = s"$printedName [--doUpdate] <input-file> [<complex_values_directory>]"
  banner(
    s"""
       |$description
       |
       |Usage:
       |
       |$synopsis
       |
       |Options:
       |""".stripMargin)

  val doUpdate: ScallopOption[Boolean] = opt[Boolean](name = "doUpdate", noshort = true,
    descr = "Without this argument no changes are made to the repository, the default is a test mode that logs the intended changes",
    default = Some(false))

  val inputFile: ScallopOption[File] = trailArg[File](name = "input-file",
    descr = "The CSV file (RFC4180) with required changes. The first line must be 'FEDORA_ID,STREAM_ID,XML_TAG,OLD_VALUE,NEW_VALUE', in that order. Additional columns and empty lines are ignored.")

  val complex_values_directory: ScallopOption[File] = trailArg[File](name = "complex_values_directory", required = false,
    descr = "A reference to a directory containing files with the complex values. These files are referenced in the input-file.")

  validateFileExists(inputFile)
  validateFileIsFile(inputFile)
  
  validateFileExists(complex_values_directory)
  validateFileIsDirectory(complex_values_directory)

  footer("")
  verify()
}
