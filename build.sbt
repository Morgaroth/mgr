name := "quide"

version := "0.1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "io.github.morgaroth" %% "morgaroth-utils-base" % "1.2.5",
  "io.github.morgaroth" %% "utils-akka" % "1.3.0",
  Ficus.Config.`1.1.2`,
  Akka.Actor.`2.3.11`
)

enablePlugins(SbtCommons)

assemblyJarName in assembly := s"${name.value}-${version.value}.jar"

// Revolver.settings