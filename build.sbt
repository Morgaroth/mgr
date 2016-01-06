name := "quide"

version := "0.1.0"

scalaVersion := "2.11.7"

resolvers ++= Seq(Repositories.Spray.releases)

libraryDependencies ++= Seq(
  "io.github.morgaroth" %% "morgaroth-utils-base" % "1.2.5",
  "io.github.morgaroth" %% "utils-akka" % "1.3.0",
  Ficus.Config.`1.1.2`,
  Akka.Actor.`2.4.1`,
  Akka.Slf4j.`2.4.1`,
  Akka.TestKit.`2.4.1`,
  Spray.Can.`1.3.3`,
  Spray.Httpx.`1.3.3`,
  Spray.Routing.`1.3.3`,
  Spray.Json.`1.3.2`,
  Spray.JsonAnnotation.`0.4.2`,
  Logback.Classic.`1.1.3`,
  ScalaTest.`2.2.4` % "test"
)

enablePlugins(SbtCommons)

assemblyJarName in assembly := s"${name.value}-${version.value}.jar"

addCompilerPlugin(Paradise.ver("2.1.0"))

// Revolver.settings