ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.1"

lazy val root = (project in file("."))
  .settings(
    name := "Haze",
    ThisBuild / libraryDependencies += "co.fs2" %% "fs2-core" % "3.9.2",
    ThisBuild / libraryDependencies += "co.fs2" %% "fs2-io" % "3.9.2"
  )
