package service

import model._
import parser.CsvParser

class DataService(countryPath: String, airportPath: String, runwayPath: String) {

  val countries: List[Country] = CsvParser.parseFile(countryPath)(Country.from)
  val airports: List[Airport] = CsvParser.parseFile(airportPath)(Airport.from)
  val runways: List[Runway] = CsvParser.parseFile(runwayPath)(Runway.from)

  private val countryMap: Map[String, Country] =
    countries.map(c => c.code.toLowerCase -> c).toMap ++
    countries.map(c => c.name.toLowerCase -> c).toMap

  private val airportsByCountry: Map[String, List[Airport]] =
    airports.groupBy(_.isoCountry.toLowerCase)

  private val runwaysByAirport: Map[Long, List[Runway]] =
    runways.groupBy(_.airportRef)

  // ----------- Public methods -----------

def findCountry(input: String): Option[Country] = {
  val lowered = input.toLowerCase
  countries.find(_.code.toLowerCase == lowered)
    .orElse(countries.find(_.name.toLowerCase == lowered))
    .orElse(countries.find(_.name.toLowerCase.contains(lowered)))
}


  def getAirports(country: Country): List[Airport] =
    airportsByCountry.getOrElse(country.code.toLowerCase, List())

  def getRunways(airportId: Long): List[Runway] =
    runwaysByAirport.getOrElse(airportId, List())

  // ----------- Reports -----------

  def top10CountriesWithMostAirports(): List[(Country, Int)] =
    airports.groupBy(_.isoCountry).toList
      .map { case (code, list) => (countries.find(_.code == code), list.size) }
      .collect { case (Some(country), count) => (country, count) }
      .sortBy(-_._2)
      .take(10)

  def top10CountriesWithLeastAirports(): List[(Country, Int)] =
  airports.groupBy(_.isoCountry).toList
    .map { case (code, list) => (countries.find(_.code == code), list.size) }
    .collect { case (Some(country), count) => (country, count) }
    .sortBy(_._2)
    .take(10)
    
  def runwayTypesPerCountry(): Map[String, Set[String]] =
    airportsByCountry.map { case (countryCode, airportList) =>
      val runwayTypes = airportList.flatMap(a => runwaysByAirport.getOrElse(a.id, Nil).map(_.surface)).toSet
      countryCode -> runwayTypes
    }

  def top10MostCommonRunwayIdent(): List[(String, Int)] =
    runways.map(_.leIdent)
      .filter(_.nonEmpty)
      .groupBy(identity)
      .view.mapValues(_.size)
      .toList.sortBy(-_._2)
      .take(10)
}
