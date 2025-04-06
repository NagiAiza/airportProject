package parser

import scala.io.Source

//for C.1
object CsvParser {

  def parseFile[T](path: String)(convert: String => Option[T]): List[T] = {
    val bufferedSource = Source.fromFile(path) // open the file
    val result = bufferedSource.getLines().drop(1).flatMap(convert).toList // skip header and parse lines
    bufferedSource.close() // close the file
    result // return the result
  }
}
