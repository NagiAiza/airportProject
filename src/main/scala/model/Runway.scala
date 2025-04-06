package model

case class Runway(id: Long, airportRef: Long, surface: String, leIdent: String)

object Runway {
  def from(line: String): Option[Runway] = {
    line.split(",", -1).toList match {
      case idStr :: airportRefStr :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: surface :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: leIdent :: _ =>
        for {
          id <- idStr.toLongOption
          airportRef <- airportRefStr.toLongOption
        } yield Runway(id, airportRef, surface.trim, leIdent.trim)
      case _ => None
    }
  }
}
