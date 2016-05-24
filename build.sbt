name := "mgr-new"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  Spray.Routing.`1.3.3`,
  Spray.Can.`1.3.3`,
  Spray.Json.`1.3.2`,
  Akka.Actor.ver("2.4.6"),
  Akka.Slf4j.ver("2.4.6"),
  Akka.TestKit.ver("2.4.6"),
  Logback.Classic.`1.1.3`,
  Ficus.Config.`1.1.2`,
  ScalaTest.last,
  Pathikrit.BetterFiles.ver("2.16.0"),
  "io.github.morgaroth" %% "utils-akka" % "1.3.0"
)

//assemblyJarName in assembly :=  s"${name.value}-${version.value}.jar"

//mainClass in assembly := Some("io.github.morgaroth.quide.core.Application")

mainClass in assembly := Some("io.github.morgaroth.quide.tests.TimeTest")

assemblyJarName in assembly := s"mgr-core-tests-${version.value}.jar"