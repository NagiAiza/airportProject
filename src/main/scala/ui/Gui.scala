package ui

import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.control._
import scalafx.scene.layout.{VBox, HBox}
import scalafx.geometry.Insets
import service.DataService
import scalafx.scene.control.TabPane.TabClosingPolicy

class Gui(dataService: DataService) extends JFXApp3 {

  override def start(): Unit = {

    // === Deux zones d'affichage séparées ===
    val queryResultArea = new TextArea {
      editable = false
      wrapText = true
      prefHeight = 400
    }

    val reportResultArea = new TextArea {
      editable = false
      wrapText = true
      prefHeight = 400
    }

    // === Onglet Query ===
    val inputField = new TextField {
      promptText = "Enter country name or code"
    }

    val searchButton = new Button("Search")
    searchButton.onAction = _ => {
      val input = inputField.text.value.trim
      val output = new StringBuilder
      dataService.findCountry(input) match {
        case Some(country) =>
          output.append(s"Airports in ${country.name}:\n\n")
          val airports = dataService.getAirports(country)
          if (airports.isEmpty) output.append("No airports found.\n")
          else airports.foreach { a =>
            output.append(s"- ${a.name} (${a.ident})\n")
            val runways = dataService.getRunways(a.id)
            runways.foreach(r => output.append(s"   • Runway: surface=${r.surface}, le_ident=${r.leIdent}\n"))
          }
        case None => output.append("Country not found.\n")
      }
      queryResultArea.text = output.toString()
    }

    val searchBox = new VBox {
      spacing = 10
      padding = Insets(10)
      children = Seq(
        new Label("Country Search"),
        new HBox {
          spacing = 5
          children = Seq(inputField, searchButton)
        }
      )
    }

    val searchTab = new Tab {
      text = "Query"
      content = new VBox {
        spacing = 10
        padding = Insets(10)
        children = Seq(searchBox, queryResultArea)
      }
    }

    // === Onglet Reports ===
    val reportChoice = new ComboBox[String](Seq(
      "Top 10 countries with most airports",
      "Top 10 countries with fewest airports",
      "Runway types per country",
      "Top 10 most common runway le_ident"
    )) {
      promptText = "Choose a report"
    }

    val reportButton = new Button("Generate")
    reportButton.onAction = _ => {
      val selected = reportChoice.value.value
      val output = new StringBuilder
      selected match {
        case "Top 10 countries with most airports" =>
          dataService.top10CountriesWithMostAirports().foreach { case (c, count) =>
            output.append(s"${c.name}: $count airports\n")
          }

        case "Top 10 countries with fewest airports" =>
          dataService.top10CountriesWithLeastAirports().foreach { case (c, count) =>
            output.append(s"${c.name}: $count airports\n")
          }

        case "Runway types per country" =>
          dataService.runwayTypesPerCountry().foreach { case (code, types) =>
            output.append(s"$code: ${types.mkString(", ")}\n")
          }

        case "Top 10 most common runway le_ident" =>
          dataService.top10MostCommonRunwayIdent().foreach { case (ident, count) =>
            output.append(s"$ident: $count times\n")
          }

        case _ => output.append("Select a valid report.")
      }
      reportResultArea.text = output.toString()
    }

    val reportTab = new Tab {
      text = "Reports"
      content = new VBox {
        spacing = 10
        padding = Insets(10)
        children = Seq(
          new Label("Available Reports"),
          new HBox {
            spacing = 10
            children = Seq(reportChoice, reportButton)
          },
          reportResultArea
        )
      }
    }

    // === Interface avec onglets ===
    val tabPane = new TabPane {
      tabs = Seq(searchTab, reportTab)
      tabClosingPolicy = TabClosingPolicy.Unavailable
    }

    stage = new JFXApp3.PrimaryStage {
      title = "Airport & Runway Explorer"
      scene = new Scene(700, 500) {
        root = tabPane
      }
    }
  }
}
