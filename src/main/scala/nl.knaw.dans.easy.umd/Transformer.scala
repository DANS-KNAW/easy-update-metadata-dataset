/*
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

  def apply(streamID: String, tagOrig: String, oldValue: String, newValue: String, isFirstDatesetStateChange: Boolean = false): RuleTransformer = {
    val (prefix, tag) = getPrefixAndTag(tagOrig)
    (streamID, tag) match {
      case ("AMD", "datasetState") => datasetStateTransformer(oldValue, newValue)
      case ("EMD", "orgISNI") => organisationIdTransformer(oldValue, "http://isni.org", "http://isni.org/isni/" + newValue.replaceAll(" ", ""), "ISNI")
      case ("EMD", "orgROR") => organisationIdTransformer(oldValue, "https://ror.org", "https://" + newValue, "ROR")
      case ("EMD", "rights") if oldValue == EMPTY => addLicenseTransformer(prefix, tag, XML.loadString(newValue))
      case _ if oldValue == EMPTY => addChildTransformer(prefix, tag, XML.loadString(newValue))
      case _ => plainTransformer(prefix, tag, oldValue, newValue)
    }
  }

  def getPrefixAndTag(tag: String): (String, String) = {
    val a = tag.split(":")
    if (a.length > 1)
      (a(0).trim, a(1).trim)
    else
      ("", a(0).trim)
  }

  def validate(streamID: String, tag: String, expectedOldValue: String, oldXML: Elem): Try[Unit] = {
    (streamID, tag) match {
      case ("AMD", "datasetState") =>
        (streamID, tag, (oldXML \ "datasetState").text) match {
          case ("AMD", "datasetState", `expectedOldValue`) =>
            Success(())
          case ("AMD", "datasetState", actualOldValue) =>
            Failure(new NotImplementedException(s"expected AMD <datasetState> [$expectedOldValue] but found [$actualOldValue]."))
          case _ =>
            Success(())
        }
      case ("EMD", "orgISNI") =>
        if ((oldXML \\ "organization").exists(_.text == expectedOldValue)) Success(())
        else Failure(new NotImplementedError(s"no organization with name [$expectedOldValue] found."))
      case _ =>
        Success(())
    }
  }

  private def addChildTransformer(prefix: String, label: String, newChild: Node): RuleTransformer =
    if (prefix.isEmpty)
      new RuleTransformer(new RewriteRule {
        override def transform(n: Node): Seq[Node] = n match {
          case Elem(prefix, `label`, attribs, scope, child @ _*) if !childExists(child, newChild) => Elem(prefix, label, attribs, scope, false, child ++ newChild: _*)
          case other => other
        }
      })
    else
      new RuleTransformer(new RewriteRule {
        override def transform(n: Node): Seq[Node] = n match {
          case Elem(`prefix`, `label`, attribs, scope, child @ _*) if !childExists(child, newChild) => Elem(prefix, label, attribs, scope, false, child ++ newChild: _*)
          case other => other
        }
      })

  private def addLicenseTransformer(prefix: String, label: String, newChild: Node): RuleTransformer =
    if (prefix.isEmpty)
      new RuleTransformer(new RewriteRule {
        override def transform(n: Node): Seq[Node] = n match {
          case Elem(prefix, `label`, attribs, scope, child @ _*) if !childExists(child, newChild) =>
            Elem(prefix, label, attribs, scope, false, childrenWithNewLicense(child, newChild): _*)
          case other => other
        }
      })
    else
      new RuleTransformer(new RewriteRule {
        override def transform(n: Node): Seq[Node] = n match {
          case Elem(`prefix`, `label`, attribs, scope, child @ _*) if !childExists(child, newChild) =>
            Elem(prefix, label, attribs, scope, false, childrenWithNewLicense(child, newChild): _*)
          case other => other
        }
      })

  private def childExists(child: Seq[Node], newChild: Node): Boolean = {
    (child contains newChild) || {
      val childS = child.foldLeft("")(_ + _.toString.replaceAll("\\s+", ""))
      val newChildS = newChild.foldLeft("")(_ + _.toString.replaceAll("\\s+", ""))
      childS contains newChildS
    }
  }

  private def childrenWithNewLicense(children: Seq[Node], newChild: Node): Seq[Node] = {
    var newChildren = Seq[Node]()
    var added: Boolean = false
    for (child <- children) {
      if (child.label == "rightsHolder" && !added) {
        newChildren = newChildren :+ newChild
        added = true
      }
      newChildren = newChildren :+ child
    }
    if (!added) {
      newChildren = newChildren :+ newChild
    }
    newChildren
  }

  private def plainTransformer(prefix: String, label: String, oldValue: String, newValue: String): RuleTransformer =
    if (prefix.isEmpty)
      new RuleTransformer(new RewriteRule {
        override def transform(n: Node): Seq[Node] = n match {
          case Elem(_, `label`, _, _, _) if newValue == EMPTY && n.text == oldValue =>
            NodeSeq.Empty
          case Elem(prefix, `label`, attribs, scope, _) if n.text == oldValue =>
            Elem(prefix, label, attribs, scope, false, Text(newValue))
          case other => other
        }
      })
    else
      new RuleTransformer(new RewriteRule {
        override def transform(n: Node): Seq[Node] = n match {
          case Elem(`prefix`, `label`, _, _, _) if newValue == EMPTY && n.text == oldValue =>
            NodeSeq.Empty
          case Elem(`prefix`, `label`, attribs, scope, _) if n.text == oldValue =>
            Elem(prefix, label, attribs, scope, false, Text(newValue))
          case other => other
        }
      })

  private def datasetStateTransformer(oldState: String, newState: String) = {
    new RuleTransformer(new RewriteRule {
      private val now: String = DateTime.now().toString
      override def transform(n: Node): Seq[Node] = n match {
        case Elem(_, "datasetState", _, _, _) =>
          <datasetState>{ newState }</datasetState>
          <previousState>{ oldState }</previousState>
          <lastStateChange>{ now }</lastStateChange>
        case Elem(_, "previousState", _, _, _) |
             Elem(_, "lastStateChange", _, _, _) =>
          NodeSeq.Empty // these might or might not have been present
        case Elem(prefix, "stateChangeDates", attribs, scope, children @ _*) =>
          Elem(prefix, "stateChangeDates", attribs, scope, false, children ++ newChangeDate(oldState, newState, now): _*)
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

  private def newChangeDate(oldState: String, newState: String, now: String): Elem = {
    <damd:stateChangeDate>
      <fromState>{oldState}</fromState>
      <toState>{newState}</toState>
      <changeDate>{ now }</changeDate>
    </damd:stateChangeDate>
  }
}
