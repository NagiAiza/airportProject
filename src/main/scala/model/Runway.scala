package model

case class Runway(id: Long, airportRef: Long, surface: String, leIdent: String)

//For C.1
object Runway {
  def from(line: String): Option[Runway] = {
    val parts = line.split(",", -1).map(_.trim.stripPrefix("\"").stripSuffix("\""))
    if (parts.length >= 20) {
      for {
        id <- parts(0).toLongOption 
        airportRef <- parts(1).toLongOption 
      } yield Runway(
        id,
        airportRef,
        parts(5),   // surface
        parts(8)    // le_ident : runway latitude
      )
    } else {
      println(s"Failed to parse runway line: $line")
      None
    }
  }
}
