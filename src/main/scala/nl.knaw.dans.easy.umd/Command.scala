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

import com.yourmediashelf.fedora.client.FedoraCredentials
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

object Command extends App with DebugEnhancedLogging {

  val configuration = Configuration()
  val clo = new CommandLineOptions(args, configuration)
  implicit val settings: Parameters = Parameters(
    test = !clo.doUpdate(),
    fedoraCredentials = new FedoraCredentials(
      configuration.properties.getString("default.fcrepo-server"),
      configuration.properties.getString("default.fcrepo-user"),
      configuration.properties.getString("default.fcrepo-password")
    ),
    input = clo.inputFile())
  logger.info(settings.toString)

  UpdateMetadataDataset.run
    .doIfSuccess(_ => println("OK: All completed successfully"))
    .doIfFailure { case e => println(s"FAILED: ${ e.getMessage }") }
}
