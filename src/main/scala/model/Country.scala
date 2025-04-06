package model

case class Country(code: String, name: String)

object Country {
  def from(line: String): Option[Country] = {
    val parts = line.split(",", -1).map(_.trim.stripPrefix("\"").stripSuffix("\""))
    if (parts.length >= 2) {
      Some(Country(parts(1), parts(2)))
    } else {
      println(s"[WARN] Failed to parse country line: $line")
      None
    }
  }
}