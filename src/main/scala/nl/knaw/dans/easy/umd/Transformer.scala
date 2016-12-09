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

import org.apache.commons.lang.NotImplementedException
import org.joda.time.DateTime

import scala.util.{Failure, Success, Try}
import scala.xml._
import scala.xml.transform.{RewriteRule, RuleTransformer}

object Transformer {

  def apply(streamID: String, tag: String, oldXML: Elem, newValue: String): Try[RuleTransformer] = {

    lazy val previousState = (oldXML \ "previousState").text
    (streamID, tag, previousState) match {
      case ("AMD", "datasetState", "") =>
        Failure(new NotImplementedException("no <previousState> available while trying to change <datasetState>."))
      case ("AMD", "datasetState", _) =>
        Success(datasetStateTransformer((oldXML \ "datasetState").text, newValue))
      case _ =>
        Success(plainTransformer(tag, newValue))
    }
  }

  private def plainTransformer(label: String, newValue: String): RuleTransformer =

    new RuleTransformer(new RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case Elem(prefix, `label`, attribs, scope, _) =>
          Elem(prefix, label, attribs, scope, false, Text(newValue))
        case other =>
          other
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
        case Elem(prefix, "stateChangeDates", attribs, scope, children@_*) =>
          Elem(prefix, "stateChangeDates", attribs, scope, false, children ++ newChangeDate(oldState, newState): _*)
        case other =>
          other
      }
    })
  }

  private def newChangeDate(oldState: String, newState: String): Elem = {
    <damd:stateChangeDate>
      <fromState>{oldState}</fromState>
      <toState>{newState}</toState>
      <changeDate>{DateTime.now().toString}</changeDate>
    </damd:stateChangeDate>
  }
}
