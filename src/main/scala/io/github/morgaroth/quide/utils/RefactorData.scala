package io.github.morgaroth.quide.utils

import better.files.Cmds._

import scala.language.{implicitConversions, reflectiveCalls}
import scala.math.{pow, sqrt}

/**
  * Created by morgaroth on 27.11.16.
  */
object RefactorData {
  implicit def gfeds(data: List[Double]): {def stats: List[Double]} = new {
    def stats: List[Double] = {
      val n = data.size
      val mean = data.sum / n
      val deriveration = sqrt(data.map(_ - mean).map(x => pow(x, 2)).sum / (n - 1))
      val error = deriveration / sqrt(n)
      List(mean, deriveration, error)
    }
  }


  def main(args: Array[String]): Unit = {
    val dataFile = pwd / "data.data"
    val data = dataFile.lines.map(_.split(":").toList).toList
    val types = data.groupBy(_.head).mapValues(_.map(_.tail))
    val fullParsedData = types.mapValues { da =>
      val registers = da.groupBy(_.head).mapValues(_.map(_.tail))
      val reg2 = registers.mapValues(_.groupBy(_.head.toInt).mapValues(_.map(_.last.toDouble).filter(_ > 0)))
      reg2.mapValues(_.mapValues(_.stats))
    }
    fullParsedData.map {
      case (kind, results) =>
        val flatten = results.zipWithIndex.flatMap { case ((register, res), idx) =>
          res.toList.sortBy(_._1).map(x => register :: (x._1 - idx * 1.0 / 15).toString :: x._2.map(_.toString) mkString ",")
        }.toSeq
        (pwd / "data" / s"$kind.csv")
          .delete(swallowIOExceptions = true)
          .createIfNotExists(createParents = true)
          .appendLines(flatten: _*)
    }
  }
}
