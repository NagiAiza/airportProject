package service

import scala.collection.parallel.CollectionConverters._

import model._
import parser.CsvParser

import java.sql.{Connection, ResultSet}

class DataService(useDatabase: Boolean) {
  if (useDatabase) println("Using H2 database")
  else println("Using Scala collections")


  val connection: Connection = DatabaseService.connection

  // chargement des données depuis les CSV
  val countries: List[Country] =
    if (!useDatabase) CsvParser.parseFile("src/main/resources/countries.csv")(Country.from) else Nil

  val airports: List[Airport] =
    if (!useDatabase) CsvParser.parseFile("src/main/resources/airports.csv")(Airport.from) else Nil

  val runways: List[Runway] =
    if (!useDatabase) CsvParser.parseFile("src/main/resources/runways.csv")(Runway.from) else Nil

  // Map of airports by country code
  private val airportsByCountry: Map[String, List[Airport]] =
    airports.groupBy(_.isoCountry.toLowerCase)

  // Map of runways by airport ID
  private val runwaysByAirport: Map[Long, List[Runway]] =
    runways.groupBy(_.airportRef)
  //

  // ----------- Public methods -----------
  // For M.2.1
  def findCountry(input: String): Option[Country] = {
    val lowered = input.toLowerCase

    if (!useDatabase) { //we use the scala list if useDatabase is false
      countries.find(c =>
        c.code.equalsIgnoreCase(lowered) || c.name.toLowerCase == lowered
      ).orElse {
        // for C.1
        val fuzzyMatches = countries
          .map(c => (c, levenshtein(lowered, c.name.toLowerCase)))
          .filter(_._2 <= 3)
          .sortBy(_._2)

        fuzzyMatches.headOption.map(_._1)
      }
    } else { // we do the sql request if useDatabase is true
      val stmt = connection.prepareStatement(
        "SELECT code, name FROM countries WHERE LOWER(code) = ? OR LOWER(name) = ?"
      )
      stmt.setString(1, lowered)
      stmt.setString(2, lowered)
      val rs = stmt.executeQuery()
      if (rs.next()) Some(Country(rs.getString("code"), rs.getString("name")))
      else {
        val all = getAllCountriesFromDB()
        val fuzzyMatches = all
          .map(c => (c, levenshtein(lowered, c.name.toLowerCase)))
          .filter(_._2 <= 3)
          .sortBy(_._2)
        fuzzyMatches.headOption.map(_._1)
      }
    }
  }

  def getAirports(country: Country): List[Airport] = {
    if (!useDatabase) {
      airportsByCountry.getOrElse(country.code.toLowerCase, List())
    } else {
      val stmt = connection.prepareStatement(
        "SELECT id, ident, name, iso_country FROM airports WHERE iso_country = ?"
      )
      stmt.setString(1, country.code)
      val rs = stmt.executeQuery()
      resultSetToList(rs, rs => Airport(
        rs.getLong("id"),
        rs.getString("ident"),
        rs.getString("name"),
        rs.getString("iso_country")
      ))
    }
  }

  def getRunways(airportId: Long): List[Runway] = {
    if (!useDatabase) {
      runwaysByAirport.getOrElse(airportId, List())
    } else {
      val stmt = connection.prepareStatement(
        "SELECT id, airport_ref, surface, le_ident FROM runways WHERE airport_ref = ?"
      )
      stmt.setLong(1, airportId)
      val rs = stmt.executeQuery()
      resultSetToList(rs, rs => Runway(
        rs.getLong("id"),
        rs.getLong("airport_ref"),
        rs.getString("surface"),
        rs.getString("le_ident")
      ))
    }
  }

  def top10CountriesWithMostAirports(): List[(Country, Int)] = {
    if (!useDatabase) {
      airports.groupBy(_.isoCountry).toList
        .map { case (code, list) => (countries.find(_.code == code), list.size) }
        .collect { case (Some(country), count) => (country, count) }
        .sortBy(-_._2)
        .take(10)
    } else {
      val stmt = connection.createStatement()
      val rs = stmt.executeQuery(
        """
          |SELECT c.code, c.name, COUNT(a.id) AS airport_count
          |FROM countries c
          |JOIN airports a ON c.code = a.iso_country
          |GROUP BY c.code, c.name
          |ORDER BY airport_count DESC
          |LIMIT 10
          |""".stripMargin)

      resultSetToList(rs, rs =>
        (Country(rs.getString("code"), rs.getString("name")), rs.getInt("airport_count"))
      )
    }
  }

  def top10CountriesWithLeastAirports(): List[(Country, Int)] = {
    if (!useDatabase) {
      airports.groupBy(_.isoCountry).toList
        .map { case (code, list) => (countries.find(_.code == code), list.size) }
        .collect { case (Some(country), count) => (country, count) }
        .sortBy(_._2)
        .take(10)
    } else {
      val stmt = connection.createStatement()
      val rs = stmt.executeQuery(
        """
          |SELECT c.code, c.name, COUNT(a.id) AS airport_count
          |FROM countries c
          |LEFT JOIN airports a ON c.code = a.iso_country
          |GROUP BY c.code, c.name
          |ORDER BY airport_count ASC
          |LIMIT 10
          |""".stripMargin)

      resultSetToList(rs, rs =>
        (Country(rs.getString("code"), rs.getString("name")), rs.getInt("airport_count"))
      )
    }
  }

  def runwayTypesPerCountry(): Map[String, Set[String]] = {
    if (!useDatabase) {
      airportsByCountry.map { case (countryCode, airportList) =>
        val runwayTypes = airportList
          .flatMap(a => runwaysByAirport.getOrElse(a.id, Nil).map(_.surface))
          .toSet
        countryCode -> runwayTypes
      }
    } else {
      val stmt = connection.createStatement()
      val rs = stmt.executeQuery(
        """
          |SELECT a.iso_country, r.surface
          |FROM airports a
          |JOIN runways r ON a.id = r.airport_ref
          |WHERE r.surface IS NOT NULL
          |""".stripMargin)

      val buffer = scala.collection.mutable.Map[String, Set[String]]().withDefaultValue(Set.empty)
      while (rs.next()) {
        val country = rs.getString("iso_country")
        val surface = rs.getString("surface")
        buffer(country) = buffer(country) + surface
      }
      buffer.toMap
    }
  }

  def top10MostCommonRunwayIdent(): List[(String, Int)] = {
    if (!useDatabase) {
      runways.par // use of parallel for C.6
        .map(_.leIdent)
        .filter(_.nonEmpty)
        .groupBy(identity _)
        .map { case (id, list) => id -> list.size }
        .toList
        .sortBy(-_._2)
        .take(10)
    } else {
      val stmt = connection.createStatement()
      val rs = stmt.executeQuery(
        """
          |SELECT le_ident, COUNT(*) AS count
          |FROM runways
          |WHERE le_ident IS NOT NULL AND le_ident <> ''
          |GROUP BY le_ident
          |ORDER BY count DESC
          |LIMIT 10
          |""".stripMargin)

      resultSetToList(rs, rs => (rs.getString("le_ident"), rs.getInt("count")))
    }
  }




  // function to Help 

  private def resultSetToList[T](rs: ResultSet, f: ResultSet => T): List[T] =
    Iterator.continually(rs)
      .takeWhile(_.next())
      .map(f)
      .toList

  private def getAllCountriesFromDB(): List[Country] = {
    val stmt = connection.createStatement()
    val rs = stmt.executeQuery("SELECT code, name FROM countries")
    resultSetToList(rs, r => Country(r.getString("code"), r.getString("name")))
  }
// For C.1
  private def levenshtein(a: String, b: String): Int = {
    val dp = Array.tabulate(a.length + 1, b.length + 1) { (i, j) =>
      if (i == 0) j
      else if (j == 0) i
      else 0
    }

    for {
      i <- 1 to a.length
      j <- 1 to b.length
    } {
      val cost = if (a(i - 1) == b(j - 1)) 0 else 1
      dp(i)(j) = List(
        dp(i - 1)(j) + 1,
        dp(i)(j - 1) + 1,
        dp(i - 1)(j - 1) + cost
      ).min
    }

    dp(a.length)(b.length)
  }
}




// package service

// import model._
// import parser.CsvParser

// class DataService(countryPath: String, airportPath: String, runwayPath: String) {

//   val countries: List[Country] = CsvParser.parseFile(countryPath)(Country.from)
//   val airports: List[Airport] = CsvParser.parseFile(airportPath)(Airport.from)
//   val runways: List[Runway] = CsvParser.parseFile(runwayPath)(Runway.from)

//   private val countryMap: Map[String, Country] =
//     countries.map(c => c.code.toLowerCase -> c).toMap ++
//     countries.map(c => c.name.toLowerCase -> c).toMap

//   private val airportsByCountry: Map[String, List[Airport]] =
//     airports.groupBy(_.isoCountry.toLowerCase)

//   private val runwaysByAirport: Map[Long, List[Runway]] =
//     runways.groupBy(_.airportRef)

//   // ----------- Public methods -----------
// // For 2.2

// /*def findCountry(input: String): Option[Country] = {
//   val lowered = input.toLowerCase
//   countries.find(_.code.toLowerCase == lowered)
//     .orElse(countries.find(_.name.toLowerCase == lowered))
//     .orElse(countries.find(_.name.toLowerCase.contains(lowered)))
// }*/

// // For C.1
// def findCountry(input: String): Option[Country] = {
//   val lowered = input.toLowerCase

//   // Match exact code ou nom
//   countries.find(c =>
//     c.code.equalsIgnoreCase(lowered) || c.name.toLowerCase == lowered
//   ).orElse {
//     // Match partiel
//     val fuzzyMatches = countries
//       .map(c => (c, levenshtein(lowered, c.name.toLowerCase)))
//       .filter(_._2 <= 3) // tolérance : 3 caractères de différence
//       .sortBy(_._2)

//     fuzzyMatches.headOption.map(_._1)
//   }
// }



//   def getAirports(country: Country): List[Airport] =
//     airportsByCountry.getOrElse(country.code.toLowerCase, List())

//   def getRunways(airportId: Long): List[Runway] =
//     runwaysByAirport.getOrElse(airportId, List())

//   // ----------- Reports -----------

//   def top10CountriesWithMostAirports(): List[(Country, Int)] =
//     airports.groupBy(_.isoCountry).toList
//       .map { case (code, list) => (countries.find(_.code == code), list.size) }
//       .collect { case (Some(country), count) => (country, count) }
//       .sortBy(-_._2)
//       .take(10)

//   def top10CountriesWithLeastAirports(): List[(Country, Int)] =
//   airports.groupBy(_.isoCountry).toList
//     .map { case (code, list) => (countries.find(_.code == code), list.size) }
//     .collect { case (Some(country), count) => (country, count) }
//     .sortBy(_._2)
//     .take(10)
    
//   def runwayTypesPerCountry(): Map[String, Set[String]] =
//     airportsByCountry.map { case (countryCode, airportList) =>
//       val runwayTypes = airportList.flatMap(a => runwaysByAirport.getOrElse(a.id, Nil).map(_.surface)).toSet
//       countryCode -> runwayTypes
//     }

//   def top10MostCommonRunwayIdent(): List[(String, Int)] =
//     runways.map(_.leIdent)
//       .filter(_.nonEmpty)
//       .groupBy(identity)
//       .view.mapValues(_.size)
//       .toList.sortBy(-_._2)
//       .take(10)


// //FOR C.1
//   private def levenshtein(a: String, b: String): Int = {
//     val dp = Array.tabulate(a.length + 1, b.length + 1) { (i, j) =>
//       if (i == 0) j
//       else if (j == 0) i
//       else 0
//     }

//     for {
//       i <- 1 to a.length
//       j <- 1 to b.length
//     } {
//       val cost = if (a(i - 1) == b(j - 1)) 0 else 1
//       dp(i)(j) = List(
//         dp(i - 1)(j) + 1,
//         dp(i)(j - 1) + 1,
//         dp(i - 1)(j - 1) + cost
//       ).min
//     }

//     dp(a.length)(b.length)
//   }

// }
