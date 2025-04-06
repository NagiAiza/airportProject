import service.DataService
import ui.Cli

object Main {
  def main(args: Array[String]): Unit = {
    val countryPath = "src/main/resources/countries.csv"
    val airportPath = "src/main/resources/airports.csv"
    val runwayPath  = "src/main/resources/runways.csv"

    val dataService = new DataService(countryPath, airportPath, runwayPath)
    println("== DEBUG AIRPORT MATCHING ==")
    val france = dataService.findCountry("FR")
    france.foreach { c =>
      val a = dataService.getAirports(c)
      println(s"Found ${a.length} airports for ${c.name}")
    }
    Cli.start(dataService)
  }
}
