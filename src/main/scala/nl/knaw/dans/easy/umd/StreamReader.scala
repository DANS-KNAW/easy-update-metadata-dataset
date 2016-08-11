package nl.knaw.dans.easy.umd

import java.io.InputStream

import com.yourmediashelf.fedora.client.FedoraClient._
import com.yourmediashelf.fedora.client.FedoraClientException
import com.yourmediashelf.fedora.client.request.RiSearch
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory

import scala.io.Source
import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, Node, XML}

object StreamReader {

  val log = LoggerFactory.getLogger(getClass)

  def getXml(pid: String, streamId: String): Try[Elem] = {
    Try(XML.load(
      // TODO close stream resulting from execute
      getDatastreamDissemination(pid, streamId).execute().getEntityInputStream
    ))
  }
}