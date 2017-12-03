package ruc.bookgraph.ebook

import java.io.File

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDDestinationNameTreeNode

import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.{PDNamedDestination, PDPageDestination}
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * 一本PDF格式的电子书
  *
  * @param pdfFile
  */
class PdfBook(pdfFile: File) extends AutoCloseable {
  /** 通过文件名称实例化PDF电子书 */
  def this(pdfFileName: String) = this(new File(pdfFileName))

  val document: PDDocument = PDDocument.load(pdfFile)

  val catalog = document.getDocumentCatalog

  import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary

  val namesDict: PDDocumentNameDictionary = catalog.getNames

  val destsTree = namesDict.getDests

  val outline = catalog.getDocumentOutline

  val docInfo = document.getDocumentInformation

  //总页数
  val pageCount = document.getPages.getCount


  val title = docInfo.getTitle
  val subject = docInfo.getSubject
  val author = docInfo.getAuthor
  val keywords = docInfo.getKeywords
  val creationDate = docInfo.getCreationDate

  val nodeOnes = fixNavTree(buildNavTree(None, outline.children().asScala.toList, 1))

  val navTree = new NavTree(nodeOnes)

  /**
    * 获取一个大纲条目对应的页码
    *
    * @param item
    * @return
    */
  def retrievePageNumber(item: PDOutlineItem): (Int, Option[String]) = {
    val goto = item.getAction
    val actionGoTo = goto.asInstanceOf[PDActionGoTo]

    val destination = actionGoTo.getDestination
    if (destination.isInstanceOf[PDNamedDestination]) {
      val namedDest = destination.asInstanceOf[PDNamedDestination]
      val pageDestination = destsTree.getValue(namedDest.getNamedDestination)
      val refName = namedDest.getNamedDestination

      (pageDestination.retrievePageNumber(), Some(refName))
    } else {
      val n = destination.asInstanceOf[PDPageDestination].retrievePageNumber()
      println(item.getTitle + "  -- " + n)
      (n, None)
    }
  }

  def getText(startPage: Int, endPage: Int): String = {
    import org.apache.pdfbox.text.PDFTextStripper
    val stripper = new PDFTextStripper
    stripper.setSortByPosition(true)
    stripper.setStartPage(startPage)
    stripper.setEndPage(endPage)
    stripper.getText(document)
  }

  /**
    * 递归把所有大纲的章节信息保存到NavNode中，记录了章节之间的父子关系和对应的页码范围。
    * 如果一个章节目录隶属于该级别的最后一个，则最后的页码赋值为-1. 后面进一步修正。
    *
    * @param parent
    * @param items
    * @param depth
    * @return
    */
  private def buildNavTree(parent: Option[NavNode],
                           items: List[PDOutlineItem],
                           depth: Int): List[NavNode] =
    items.map {
      item: PDOutlineItem =>
        val title = item.getTitle.replaceAll("\n", " ")
        val (startPage, startRefName) = retrievePageNumber(item)

        val (endPage, _) = if (item.getNextSibling != null)
          retrievePageNumber(item.getNextSibling)
        else
          (-1, None)

        //如果还有子节点，则把子节点连接起来
        val childItems = item.children().asScala.toList

        val node = NavNode(title, startRefName, depth, startPage, endPage, parent)

        if (childItems.nonEmpty) {
          NavNode(title, startRefName, depth, startPage, endPage, parent,
            buildNavTree(Some(node), item.children().asScala.toList, depth + 1)
          )
        } else {
          NavNode(title, startRefName, depth, startPage, endPage, parent)
        }
    }

  /**
    * 补充页码数据，同时，如果第一级目录的数量为1，则直接返回第二级目录，作为nodeOnes
    * @param nodes
    * @return
    */
  private def fixNavTree(nodes: List[NavNode]): List[NavNode] = {
    // 是否要删除de
    val removeTopNode = (nodes.size == 1 && nodes(0).children.size>1)

    //按层次遍历树，把endPage为-1的页码调整正确
    val queue = mutable.Queue.empty[NavNode]
    queue.enqueue(nodes: _*)
    while (!queue.isEmpty) {
      val node = queue.dequeue()

      if(removeTopNode) node.depth = node.depth -1

      if (node.endPage == -1 && node.parent.isEmpty) {
        node.endPage = pageCount
      } else if (node.endPage == -1 && node.parent.nonEmpty) {
        node.endPage = node.parent.get.endPage
      }
      queue.enqueue(node.children: _*)
    }

    if(removeTopNode) nodes(0).children else nodes
  }

  override def close(): Unit = {
    document.close()
  }
}
