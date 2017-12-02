package ruc.bookgraph.ebook

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class NavNode(title: String, //标题
                   depth: Int, //目录深度
                   startPage: Int, //起始页码
                   var endPage: Int, //终止页码, -1表示该级别的最后一页
                   var parent: Option[NavNode],
                   var children: List[NavNode] = List.empty[NavNode]) {

}


