package model

case class Country(code: String, name: String)

// For C.1
object Country {
  def from(line: String): Option[Country] = {
    val parts = line.split(",", -1).map(_.trim.stripPrefix("\"").stripSuffix("\""))
    if (parts.length >= 2) {
      Some(Country(parts(1), parts(2))) // 1 = code, 2 = name
    } else {
      println(s"Failed to parse country line: $line")
      None
    }
  }
}