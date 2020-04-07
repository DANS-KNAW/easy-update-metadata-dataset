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

import org.apache.commons.lang.NotImplementedException
import org.joda.time.DateTime

import scala.util.{ Failure, Success, Try }
import scala.xml._
import scala.xml.transform.{ RewriteRule, RuleTransformer }

object Transformer {

  private val EMPTY = "EMPTY"

  def apply(streamID: String, tag: String, oldValue: String, newValue: String): RuleTransformer = {
    (streamID, tag) match {
      case ("AMD", "datasetState") => datasetStateTransformer(oldValue, newValue)
      case ("EMD", "orgISNI") => organisationIdTransformer(oldValue, "http://isni.org", "http://isni.org/isni/" + newValue.replaceAll(" ", ""), "ISNI")
      case ("EMD", "orgROR") => organisationIdTransformer(oldValue, "https://ror.org", "https://" + newValue, "ROR")
      case _ => plainTransformer(tag, oldValue, newValue)
    }
  }

  def validate(streamID: String, tag: String, expectedOldValue: String, oldXML: Elem): Try[Unit] = {
    (streamID, tag) match {
      case ("AMD", "datasetState") =>
        (streamID, tag, (oldXML \ "datasetState").text, (oldXML \ "previousState").text) match {
          case ("AMD", "datasetState", _, "") =>
            Failure(new NotImplementedException("no <previousState> in AMD."))
          case ("AMD", "datasetState", `expectedOldValue`, _) =>
            Success(())
          case ("AMD", "datasetState", actualOldValue, _) =>
            Failure(new NotImplementedException(s"expected AMD <datasetState> [$expectedOldValue] but found [$actualOldValue]."))
          case _ =>
            Success(())
        }
      case ("EMD", "orgISNI") => {
        if ((oldXML \\ "organization").exists(_.text == expectedOldValue))  Success(())
        else Failure(new NotImplementedError(s"no organization with name [$expectedOldValue] found."))
      }
      case _ =>
        Success(())
    }
  }

  private def plainTransformer(label: String, oldValue: String, newValue: String): RuleTransformer =
    new RuleTransformer(new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case Elem(_, `label`, _, _, _) if newValue == EMPTY && n.text == oldValue =>
          NodeSeq.Empty
        case Elem(prefix, `label`, attribs, scope, _) if n.text == oldValue =>
          Elem(prefix, label, attribs, scope, false, Text(newValue))
        case other => other
      }
    })

  private def datasetStateTransformer(oldState: String, newState: String): RuleTransformer = {
    new RuleTransformer(new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case Elem(prefix, "datasetState", attribs, scope, _) =>
          Elem(prefix, "datasetState", attribs, scope, false, Text(newState))
        case Elem(prefix, "previousState", attribs, scope, _) =>
          Elem(prefix, "previousState", attribs, scope, false, Text(oldState))
        case Elem(prefix, "lastStateChange", attribs, scope, _) =>
          Elem(prefix, "lastStateChange", attribs, scope, false, Text(DateTime.now().toString))
        case Elem(prefix, "stateChangeDates", attribs, scope, children @ _*) =>
          Elem(prefix, "stateChangeDates", attribs, scope, false, children ++ newChangeDate(oldState, newState): _*)
        case other => other
      }
    })
  }

  private def organisationIdTransformer(organisationName: String, schemeURI: String, organisationURI: String, scheme: String): RuleTransformer = {
    new RuleTransformer(new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case e: Elem if e.label == "organization" && e.text == organisationName =>
          e ++ <eas:organizationId eas:identification-system={schemeURI} eas:scheme={scheme}>{organisationURI}</eas:organizationId>
        case other => other
      }
    })
  }

  private def newChangeDate(oldState: String, newState: String): Elem = {
    //@formatter:off
    <damd:stateChangeDate>
      <fromState>{oldState}</fromState>
      <toState>{newState}</toState>
      <changeDate>{DateTime.now().toString}</changeDate>
    </damd:stateChangeDate>
    //@formatter:on
  }
}
