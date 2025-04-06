package model

case class Country(code: String, name: String)

object Country {
  def from(line: String): Option[Country] = {
    line.split(",", -1).toList match {
      case code :: name :: _ =>
        Some(Country(code.trim.stripPrefix("\"").stripSuffix("\""), name.trim.stripPrefix("\"").stripSuffix("\"")))
      case _ => None
    }
  }
}