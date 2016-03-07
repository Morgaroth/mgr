import io.github.morgaroth.sbt.commons.SbtCommons
import io.github.morgaroth.sbt.commons.SbtCommons.autoImport._
import sbtassembly.AssemblyPlugin.autoImport._
import sbt._
import sbt.Keys._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import org.scalajs.sbtplugin.cross.CrossProject
import org.scalajs.core.tools.sem._
import spray.revolver.RevolverPlugin._
import com.typesafe.sbt.web.SbtWeb
import com.typesafe.sbt.web.Import._
import sass.Import._

object Build extends sbt.Build {
  val outPath = new File("web")
  val jsPath = outPath / "js"
  val cssPath = outPath / "css"
  val fontsPath = outPath / "fonts"

  val deploy = System.getenv("DEPLOY") == "true"
  val port = if (deploy) 80 else 8080
  val optLevel: TaskKey[Attributed[File]] =
    if (deploy) {
      println("[info] Compiling in production mode")
      fullOptJS
    } else {
      println("[info] Compiling in development mode")
      fastOptJS
    }
  val semantics: Semantics => Semantics =
    if (!deploy) identity[Semantics]
    else _
      .withRuntimeClassName(_ => "")
      .withAsInstanceOfs(CheckedBehavior.Unchecked)

  val copyFontsTask = {
    val webJars = WebKeys.webJarsDirectory in Assets
    webJars.map { path =>
      val paths = Seq(
        path / "lib" / "font-awesome" / "fonts"
        , path / "lib" / "bootstrap-sass" / "fonts" / "bootstrap"
      )

      paths.flatMap { fontPath =>
        fontPath.listFiles().map { src =>
          val tgt = fontsPath / src.getName
          IO.copyFile(src, tgt)
          tgt
        }
      }
    }
  }.dependsOn(WebKeys.webJars in Assets)

  lazy val crossProject = CrossProject("server", "client", file("."), CrossType.Full)
    .enablePlugins(SbtWeb, SbtCommons)
    .settings(
      name := "quide",
      version := "1.1",
      organization := "io.github.morgaroth.msc.quide",
      scalaVersion := "2.11.7",
      scalacOptions := Seq(
        "-unchecked"
        , "-deprecation"
        , "-encoding", "utf8"
        , "-Xelide-below", annotation.elidable.ALL.toString
      )
    )
    .jvmSettings(
      Revolver.settings: _*
    )
    .jvmSettings(
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
      ),
      assemblyJarName in assembly := {
        s"${name.value}-${version.value}.jar"
      }
    )
    .jsSettings(
      libraryDependencies ++= Seq(
        "com.lihaoyi" %%% "upickle" % "0.3.6",
        "org.webjars" % "bootstrap-sass" % "3.3.1",
        "org.webjars" % "font-awesome" % "4.3.0-1",
        "com.github.japgolly.scalajs-react" %%% "extra" % "0.10.0"
      ),
      jsDependencies ++= Seq(
        "org.webjars.npm" % "react" % "0.14.0" / "react-with-addons.js" commonJSName "React" minified "react-with-addons.min.js",
        "org.webjars.npm" % "react-dom" % "0.14.0" / "react-dom.js" commonJSName "ReactDOM" minified "react-dom.min.js" dependsOn "react-with-addons.js"
      )
      , persistLauncher := true
      , skip in packageJSDependencies := false
      , artifactPath in(Compile, packageScalaJSLauncher) := jsPath / "launcher.js"
      , artifactPath in(Compile, packageJSDependencies) := jsPath / "deps.js"
      , artifactPath in(Compile, optLevel) := jsPath / "application.js"
      , resourceManaged in sass in Assets := cssPath
      , sourceGenerators in Assets <+= copyFontsTask
      , scalaJSSemantics ~= semantics
    )

  lazy val js = crossProject.js

  lazy val jvm = if (deploy) {
    crossProject.jvm.settings(
      //    baseDirectory in Revolver.reStart := new File("jvm") / "target" // defaults to jvm/
      //    ,
      Revolver.reStart <<= Revolver.reStart dependsOn (optLevel in(js, Compile))
    )
  } else crossProject.jvm
}
