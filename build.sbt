organization := "ruc"
name					:= "bookgraph"
version := "1.0"

scalaVersion  := "2.12.1"
scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

//fork in run := true
//cancelable in Global := true

val akkaVersion = "2.5.6"

//akka
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-remote" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.5"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.5"
libraryDependencies += "com.typesafe" % "config" % "1.3.1"

//akka http 跨域访问
libraryDependencies += "ch.megard" %% "akka-http-cors" % "0.2.1"

//XML support
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
libraryDependencies += "org.scala-lang.modules" %% "scala-async" % "0.9.6"

//Scala wrapper for Joda Time.
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.16.0"

//command line parser
libraryDependencies += "com.github.scopt" %% "scopt" % "3.5.0"

libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.0.0"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"

//NLP libraries
libraryDependencies += "com.hankcs" % "hanlp" % "portable-1.2.11"
libraryDependencies += "org.ahocorasick" % "ahocorasick" % "0.3.0"

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.3.1"
libraryDependencies += "com.google.guava" % "guava" % "22.0"

//http jars
libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.2"
libraryDependencies += "org.apache.james" % "apache-mime4j-core" % "0.7.2"
libraryDependencies += "com.ibm.icu" % "icu4j" % "53.1"
libraryDependencies += "org.jsoup" % "jsoup" % "1.9.2"

// Database drivers
libraryDependencies += "com.github.etaty" %% "rediscala" % "1.8.0"
libraryDependencies +=  "org.reactivemongo" %% "reactivemongo" % "0.12.6"

// PDF library
libraryDependencies += "org.apache.pdfbox" % "pdfbox" % "2.0.8"

//Java mail
libraryDependencies += "javax.mail" % "mail"  % "1.4.7"

resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"

scalacOptions in Test ++= Seq("-Yrangepos")

//native package
import NativePackagerHelper.directory

enablePlugins(JavaAppPackaging)
mainClass in Compile := Some("ruc.netbook.Main")

enablePlugins(UniversalPlugin)
javaOptions in Universal ++= Seq(
  // -J params will be added as jvm parameters
  s"-Dpidfile.path=${packageName.value}.pid",
  "-J-Xms4G",
  "-J-Xmx8G"
)

// copy related files
mappings in Universal <++= (packageBin in Compile) map { _ =>
  /**
    * 显示一个目录下的所有文件，包括文件夹中的文件，返回文件和对应的文件名称，文件名称采用相对于prefix的相对路径
    */
  def listAllFiles(root: File, prefix: String):List[(File, String)] = {
    root.listFiles().flatMap {
      f => if(f.isDirectory)
        listAllFiles(f, prefix + f.getName + "/")
      else
        List((f, prefix + f.getName))
    }.toList
  }

  listAllFiles(new File("./web"), "web/"):::listAllFiles(new File("./conf"), "conf/")
    .map{ case (f: File, path:String) => f -> path }
}

javaOptions in Universal ++= Seq(
  // -J params will be added as jvm parameters
  "-J-Xms4G",
  "-J-Xmx8G"
)

initialCommands in console +=
  """
    |import java.io.File
    |import java.util.Date
    |
    |import org.apache.pdfbox.pdmodel.PDDocument
    |import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
    |import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
    |import ruc.bookgraph.ebook._
  """.stripMargin