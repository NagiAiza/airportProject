package model

case class Airport(id: Long, ident: String, name: String, isoCountry: String)

// For C.1
object Airport {
  def from(line: String): Option[Airport] = {
    val parts = line.split(",", -1).map(_.trim.stripPrefix("\"").stripSuffix("\""))
    if (parts.length >= 9) {
      parts(0).toLongOption.map(id =>
        Airport(
          id,
          parts(1), // ident
          parts(3), // name
          parts(8) // iso_country
        )
      )
    } else {
      println(s"Failed to parse airport line: $line")
      None
    }
  }
}
