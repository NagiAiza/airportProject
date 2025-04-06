package ui

import service.DataService
import model._

import scala.io.StdIn.readLine

object Cli {

  def start(dataService: DataService): Unit = {

    def menu(): Unit = {
      println("\nMenu :")
      println("1. Query (search airports & runways by country)")
      println("2. Reports")
      println("3. Exit")
      readLine("Enter your choice: ") match {
        case "1" => query()
        case "2" => reports()
        case "3" => println("End of Program!")
        case _   => println("Invalid input"); menu()
      }
    }

    def query(): Unit = {
      val input = readLine("\nEnter country name or code: ").trim
      dataService.findCountry(input) match {
        case Some(country) =>
          println(s"Found country: ${country.name} (${country.code})")
          val airports = dataService.getAirports(country)
          if (airports.isEmpty) println("No airport found")
          else {
            airports.foreach { airport =>
              println(s"- Airport: ${airport.name} (${airport.ident})")
              val runways = dataService.getRunways(airport.id)
              runways.foreach(r => println(s" -- Runway: surface=${r.surface}, runway latitude=${r.leIdent}"))
            }
          }
        case None => println("Country not found.")
      }
      menu()
    }

    def reports(): Unit = {
      println("\nReports :")
      println("1. Top 10 countries with most airports")
      println("2. 10 countries with fewest airports")
      println("3. Runway types per country")
      println("4. Top 10 most common runway ident (le_ident)")
      println("5. Back to main menu")
      readLine("Choose report: ") match {
        case "1" =>
          dataService.top10CountriesWithMostAirports().foreach { case (country, count) =>
            println(s"${country.name}: $count airports")
          }
          reports()
        case "2" =>
          dataService.top10CountriesWithLeastAirports().foreach { case (country, count) =>
            println(s"${country.name}: $count airports")
          }
          reports()
        case "3" =>
          dataService.runwayTypesPerCountry().foreach { case (countryCode, surfaces) =>
            println(s"$countryCode: ${surfaces.mkString(", ")}")
          }
          reports()
        case "4" =>
          dataService.top10MostCommonRunwayIdent().foreach { case (ident, count) =>
            println(s"$ident: $count times")
          }
          reports()
        case "5" => menu()
        case _   => println("Invalid choice."); reports()
      }
    }

    menu()
  }
}
