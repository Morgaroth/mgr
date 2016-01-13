package io.github.morgaroth.msc.quide.model.operators

import io.github.morgaroth.msc.quide.model.Complex._
import io.github.morgaroth.msc.quide.model.{Complex, QbitValue}

import scala.language.implicitConversions

/**
  * Created by mateusz on 04.01.16.
  */
trait Operator {
  def size: Int

  def elements: Map[MatrixPos, Complex]

  def isEmpty = elements.isEmpty

  def apply(row: Int, col: Int): Complex = elements
    .filterKeys(x => x.row == row && x.col == col)
    .values.headOption
    .getOrElse(Complex.`0`)

  lazy val columns_raw = elements.toList.map {
    case (pos, value) => (pos.col / 2) ->(pos, value)
  }.groupBy(_._1).mapValues(_.map(_._2))

  lazy val rows_raw = elements.toList.map {
    case (pos, value) => (pos.row / 2) ->(pos, value)
  }.groupBy(_._1).mapValues(_.map(_._2))

  lazy val columnSubOperators = columns_raw.mapValues(elems =>
    elems.map { case (pos, value) => (pos.row / 2) ->(pos, value) }
      .groupBy(_._1)
      .mapValues(x => SingleQbitOperator(x.map(_._2)))
  )

  lazy val rowSubOperators = rows_raw.mapValues(elems =>
    elems.map { case (pos, value) => (pos.col / 2) ->(pos, value) }
      .groupBy(_._1)
      .mapValues(x => SingleQbitOperator(x.map(_._2)))
  )

}

// TODO next operators (2, 3 qbits, SWAP, CnNOT, itd)
// without sense!
/*
  Complex operator
 */
class MatrixOperator(m: IntelligentMatrix) extends Operator {
  override def size: Int = m.size / 2

  override def elements: Map[MatrixPos, Complex] = m.elements

  override def apply(row: Int, col: Int): Complex = m.elements.getOrElse((row, col), `0`)
}

object MatrixOperator {
  def apply(a: List[List[Complex]]) = {
    val values = a.zipWithIndex.flatMap {
      case (row, rowIdx) => row.filter(_.non0).zipWithIndex.map {
        case (value, colIdx) => MatrixPos(rowIdx, colIdx) -> value
      }
    }.toMap
    new MatrixOperator(IntelligentMatrix(a.size, values))
  }
}

case class MatrixElem(row: Int, col: Int, value: Complex)

case class MatrixPos(row: Int, col: Int)

object MatrixPos {
  implicit def fromTuple(t: (Int, Int)): MatrixPos = MatrixPos(t._1, t._2)
}

case class IntelligentMatrix(size: Int, elements: Map[MatrixPos, Complex])