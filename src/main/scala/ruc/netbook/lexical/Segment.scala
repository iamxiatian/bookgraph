package nlp.lexical

import com.hankcs.hanlp.HanLP
import com.hankcs.hanlp.dictionary.CustomDictionary
import com.hankcs.hanlp.seg.Segment

import scala.collection.JavaConverters._


object Segment {

  val segment: Segment = HanLP.newSegment
    .enableNameRecognize(true)
    .enablePlaceRecognize(true)
    .enableOrganizationRecognize(true)

  CustomDictionary.add("中国人民大学", "nt 5")
  CustomDictionary.add("信息资源管理学院", "nt 5")
  CustomDictionary.add("中国人民公安大学", "nt 5")

  /**
    * 对文本进行利用HanLP进行分词和词性标注，分词结果为一个列表，列表元素为(词语，词性)
    *
    * @param text
    * @return
    */
  def segment(text: String) = HanLP.segment(text)
    .asScala
    .map(term => (term.word, term.nature.name))
    .toList

  /**
    * 对文本进行分词和词性标记，结果作为一个字符串返回
    *
    * @param text
    * @return
    */
  def segmentAsString(text: String): String = segment(text)
    .map { case (name, pos) => s"$name/$pos" }.mkString(" ")
}