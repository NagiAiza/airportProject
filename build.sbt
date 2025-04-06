// import Dependencies._

// ThisBuild / scalaVersion     := "2.13.12"
// ThisBuild / version          := "0.1.0-SNAPSHOT"
// ThisBuild / organization     := "com.example"
// ThisBuild / organizationName := "example"

// lazy val root = (project in file("."))
//   .settings(
//     name := "airportProject",
//     libraryDependencies += munit % Test
//   )

// // See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.


// libraryDependencies ++= Seq(
//   "com.h2database" % "h2" % "2.2.224",
//   "org.scala-lang.modules" %% "scala-collection-compat" % "2.11.0"
// )

ThisBuild / scalaVersion     := "2.13.12"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "airportProject",
    libraryDependencies ++= dependencies
  )

// === Dépendances ===

val javafxVersion = "17.0.2"

val osName = sys.props("os.name").toLowerCase match {
  case name if name.contains("win")  => "win"
  case name if name.contains("mac")  => "mac"
  case name if name.contains("linux") => "linux"
  case _ => throw new RuntimeException("Unsupported OS")
}

val dependencies = Seq(
  "org.scalafx" %% "scalafx" % "16.0.0-R25",
  "com.h2database" % "h2" % "2.2.224",
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.11.0",
    "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
  "org.scalameta" %% "munit" % "0.7.29" % Test
) ++ Seq("base", "graphics", "controls", "fxml", "media").map { m =>
  "org.openjfx" % s"javafx-$m" % javafxVersion classifier osName
}

// JavaFX config 

fork := true

Compile / run / javaOptions ++= Seq(
  "--module-path", (Compile / dependencyClasspath).value
    .map(_.data.getAbsolutePath)
    .filter(_.contains("javafx"))
    .mkString(java.io.File.pathSeparator),
  "--add-modules", "javafx.controls,javafx.fxml"
)
