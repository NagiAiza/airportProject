import service.{DataService, DatabaseService}
import ui.{Cli, Gui}


object Main {
  // def main(args: Array[String]): Unit = {
  //   val countryPath = "src/main/resources/countries.csv"
  //   val airportPath = "src/main/resources/airports.csv"
  //   val runwayPath  = "src/main/resources/runways.csv"

  //   val dataService = new DataService(countryPath, airportPath, runwayPath)
  //   println("== DEBUG AIRPORT MATCHING ==")
  //   val france = dataService.findCountry("FR")
  //   france.foreach { c =>
  //     val a = dataService.getAirports(c)
  //     println(s"Found ${a.length} airports for ${c.name}")
  //   }
  //   Cli.start(dataService)
  // }

  def main(args: Array[String]): Unit = {
  val countryPath = "src/main/resources/countries.csv"
  val airportPath = "src/main/resources/airports.csv"
  val runwayPath  = "src/main/resources/runways.csv"

  val countries = parser.CsvParser.parseFile(countryPath)(model.Country.from)
  val airports  = parser.CsvParser.parseFile(airportPath)(model.Airport.from)
  val runways   = parser.CsvParser.parseFile(runwayPath)(model.Runway.from)

  DatabaseService.init()
  DatabaseService.insertCountries(countries)
  DatabaseService.insertAirports(airports)
  DatabaseService.insertRunways(runways)
// to use the database h2 set useDatabase = true
  val dataService = new DataService(useDatabase = true)

  //Cli.start(dataService)
  new Gui(dataService).main(Array.empty)



}

}
