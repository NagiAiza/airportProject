package parser

import scala.io.Source

object CsvParser {

  def parseFile[T](path: String)(convert: String => Option[T]): List[T] = {
    val bufferedSource = Source.fromFile(path)
    val result = bufferedSource.getLines().drop(1).flatMap(convert).toList
    bufferedSource.close()
    result
  }
}
