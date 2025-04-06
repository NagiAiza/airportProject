package model

case class Airport(id: Long, ident: String, name: String, isoCountry: String)

object Airport {
  def from(line: String): Option[Airport] = {
    line.split(",", -1).toList match {
      case idStr :: ident :: name :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: _ :: isoCountry :: _ =>
        for {
          id <- idStr.toLongOption
        } yield Airport(id, ident.trim, name.trim, isoCountry.trim)
      case _ => None
    }
  }