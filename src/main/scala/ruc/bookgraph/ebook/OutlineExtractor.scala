package ruc.bookgraph.ebook

import java.io.File

class OutlineExtractor {
  def main(args: Array[String]): Unit = {
    //val pdfFile = new File("/home/xiatian/Documents/项目申请/教育图书进出口公司/样例/9780470073681.pdf")
    val pdfFile = new File("/home/xiatian/Documents/books/LinkedData/Learning SPARQL, 2nd Edition.pdf")
    val book = new PdfBook(pdfFile)

  }

}
