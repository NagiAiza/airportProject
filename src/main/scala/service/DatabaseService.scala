package service

import java.sql.{Connection, DriverManager}
import model._
//for C.2
object DatabaseService {
  val connection: Connection = DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1") // In-memory database

  // init method to create tables
  def init(): Unit = {
    val stmt = connection.createStatement()
    stmt.execute("CREATE TABLE countries (code VARCHAR PRIMARY KEY, name VARCHAR)")
    stmt.execute("CREATE TABLE airports (id BIGINT PRIMARY KEY, ident VARCHAR, name VARCHAR, iso_country VARCHAR)")
    stmt.execute("CREATE TABLE runways (id BIGINT PRIMARY KEY, airport_ref BIGINT, surface VARCHAR, le_ident VARCHAR)")
    stmt.close()
  }

  // insert methods to add data to tables
  def insertCountries(countries: List[Country]): Unit = {
    val prep = connection.prepareStatement("INSERT INTO countries VALUES (?, ?)")
    countries.foreach { c =>
      prep.setString(1, c.code) 
      prep.setString(2, c.name) 
      prep.addBatch()
    }
    prep.executeBatch() //execute all the batched statements
    prep.close() // close the prepared statement
  }

  def insertAirports(airports: List[Airport]): Unit = {
    val prep = connection.prepareStatement("INSERT INTO airports VALUES (?, ?, ?, ?)")
    airports.foreach { a =>
      prep.setLong(1, a.id)
      prep.setString(2, a.ident)
      prep.setString(3, a.name)
      prep.setString(4, a.isoCountry)
      prep.addBatch()
    }
    prep.executeBatch()
    prep.close()
  }

  def insertRunways(runways: List[Runway]): Unit = {
    val prep = connection.prepareStatement("INSERT INTO runways VALUES (?, ?, ?, ?)")
    runways.foreach { r =>
      prep.setLong(1, r.id)
      prep.setLong(2, r.airportRef)
      prep.setString(3, r.surface)
      prep.setString(4, r.leIdent)
      prep.addBatch()
    }
    prep.executeBatch()
    prep.close()
  }
}
