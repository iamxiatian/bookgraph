package ruc.netbook.pdf

import java.io.File
import java.util.Date

import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode
import org.apache.pdfbox.pdmodel.PDDocumentInformation
import org.apache.pdfbox.pdmodel.PDDocument

case class DocMetadata(val title: String,
                       val subject: String,
                       val author: String,
                       val keywords: String,
                       val creationDate: Date)

case class ContentItem(val title: String,
                       val startPN: Int, //Start page number
                       val endPN: Int, // End page number
                       val text: String
                      )

object OutlineExtractor {

  def getText(document: PDDocument, startPage: Int, endPage: Int): String = {
    import org.apache.pdfbox.text.PDFTextStripper
    val stripper = new PDFTextStripper
    stripper.setSortByPosition(true)
    stripper.setStartPage(startPage)
    stripper.setEndPage(endPage)
    stripper.getText(document)
  }

  /**
    * 获取第一级书签
    *
    * @param current
    * @return
    */
  private def getOutlineItems(current: PDOutlineItem): List[PDOutlineItem] = {
    if (current == null)
      List.empty[PDOutlineItem]
    else
      current :: getOutlineItems(current.getNextSibling)
  }.filter(p => p.getDestination.isInstanceOf[PDPageDestination])

  private def convertItems(document: PDDocument,
                           items: List[PDOutlineItem]
                          ): List[ContentItem] = items match {
    case first :: second :: rest =>
      val firstPD = first.getDestination.asInstanceOf[PDPageDestination]
      val nextPD = second.getDestination.asInstanceOf[PDPageDestination]
      val startPage = firstPD.getPageNumber
      val endPage = nextPD.getPageNumber

      ContentItem(
        first.getTitle,
        startPage,
        endPage,
        getText(document, startPage, endPage)
      ) :: convertItems(document, second :: rest)

    case first :: Nil =>
      val startPage = first.getDestination
        .asInstanceOf[PDPageDestination].getPageNumber
      val endPage = document.getNumberOfPages

      val text = getText(document, startPage, endPage)

      ContentItem(
        first.getTitle,
        startPage,
        endPage,
        getText(document, startPage, endPage)
      ) :: Nil
    case _ => List.empty[ContentItem]
  }

  def extract(pdfFile: File): Unit = {
    val document: PDDocument = PDDocument.load(pdfFile)
    val outline = document.getDocumentCatalog.getDocumentOutline
    val docInfo = document.getDocumentInformation

    val metadata = DocMetadata(
      docInfo.getTitle,
      docInfo.getSubject,
      docInfo.getAuthor,
      docInfo.getKeywords,
      docInfo.getCreationDate.getTime)


    val rawItems = getOutlineItems(outline.getFirstChild)
    val contentItems = convertItems(document, rawItems)

    contentItems.foreach(println)
  }

  def main(args: Array[String]): Unit = {
    extract(new File("/home/xiatian/Documents/项目申请/教育图书进出口公司/样例/9780470073681.pdf"))
  }

}
