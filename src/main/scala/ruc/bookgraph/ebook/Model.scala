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

class NavTree(
               nodeOnes: List[NavNode] //第一层目录
             ) {
  /**
    * 列出所有深度为depth的导航节点
    *
    * @param depth
    */
  def listAllNodes(depth: Int): List[NavNode] = {
    //按层次遍历树，直到深度为depth为止
    val lst = new ListBuffer[NavNode]()
    val queue = mutable.Queue.empty[NavNode]
    queue.enqueue(nodeOnes: _*)

    while (!queue.isEmpty) {
      val node = queue.dequeue()
      if (node.depth == depth) {
        lst += node
      } else if (node.depth < depth) {
        queue.enqueue(node.children: _*)
      }
    }

    lst.toList
  }

  /**
    * 寻找书的章节：
    *
    * 1. 按照书的标题，是否包含Chapter
    一章里面，应该包含若干小节。
    * 3. 章节的数量不宜过少，应该在5章以上
    * 4. 章节覆盖的页码，应该在70%以上
    * 5. 章节的数量不宜过多，平均每章的文字数量应该在一定比例以上
    */
  def findChapters(): Unit = {

  }

  def print(): String = print(nodeOnes)

  def print(node: NavNode): String = {
    ("  " * node.depth) + s"${node.title}\n" + print(node.children)
  }

  def print(nodes: List[NavNode]): String =
    nodes.zipWithIndex.map {
      case (node: NavNode, idx: Int) =>
        ("  " * node.depth) +
          s"$idx \t [${node.startPage},\t${node.endPage}] ${node.title}"
    }.mkString("\n")

}

