name := "mgr-new"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  Spray.Routing.`1.3.3`,
  Spray.Can.`1.3.3`,
  Spray.Json.`1.3.2`,
  Akka.Actor.`2.4.1`,
  Akka.Slf4j.`2.4.1`,
  Akka.TestKit.`2.4.1`,
  Logback.Classic.`1.1.3`,
  Ficus.Config.`1.1.2`,
  ScalaTest.last,
  "io.github.morgaroth" %% "utils-akka" % "1.3.0"
)

assemblyJarName in assembly := {
  s"${name.value}-${version.value}.jar"
}