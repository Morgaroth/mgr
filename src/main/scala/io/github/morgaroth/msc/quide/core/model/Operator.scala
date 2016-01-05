package io.github.morgaroth.msc.quide.core.model

import io.github.morgaroth.msc.quide.core.model.Complex._

import scala.language.implicitConversions

/**
  * Created by mateusz on 04.01.16.
  */
trait Operator {
  def size: Int

  def apply(row: Int, col: Int): Complex

  def elements: Map[MatrixPos, Complex]
}

trait SingleQbitOperator extends Operator {
  override def size: Int = 1

  def apply(qbit: QbitValue): QbitValue = {
    println("apply from base SingleQubit")
    QbitValue(this (0, 0) * qbit.`a|0>` + this (0, 1) * qbit.`b|1>`, this (1, 0) * qbit.`a|0>` + this (1, 1) * qbit.`b|1>`)
  }

}

/*
  Predefined, well-known operators
 */
trait IdentityLike extends SingleQbitOperator {
  override def apply(row: Int, col: Int) = if (row == col) 1 else 0

  override lazy val elements: Map[MatrixPos, Complex] = Map(
    MatrixPos(0, 0) -> `1`,
    MatrixPos(1, 1) -> `1`
  )

  override def apply(qbit: QbitValue): QbitValue = {
    println("apply from I")
    qbit
  }

  override def toString: String = "Identity"
}

object Intentity extends IdentityLike

object I extends IdentityLike

trait HadammardLike extends SingleQbitOperator {
  override def apply(row: Int, col: Int): Complex = {
    (row, col) match {
      case (1, 1) => `-1/p2`
      case _ => `1/p2`
    }
  }

  override lazy val elements: Map[MatrixPos, Complex] = Map(
    MatrixPos(0, 0) -> `1/p2`,
    MatrixPos(1, 0) -> `1/p2`,
    MatrixPos(0, 1) -> `1/p2`,
    MatrixPos(1, 1) -> `-1/p2`
  )

  override def toString: String = "Hadammard"
}

object Hadammard extends HadammardLike

object H extends HadammardLike

trait PauliXLike extends SingleQbitOperator {
  override def apply(row: Int, col: Int): Complex = if (row + col == 1) `1` else `0`

  override lazy val elements: Map[MatrixPos, Complex] = Map(
    MatrixPos(0, 1) -> `1`,
    MatrixPos(1, 0) -> `1`
  )

  // overriden for simplier calculations
  // Pauli X is a `bit flip` gate
  override def apply(qbit: QbitValue): QbitValue = QbitValue(qbit.`b|1>`, qbit.`a|0>`)

  override def toString: String = "PauliX"
}

object PauliX extends PauliXLike

object X extends PauliXLike

trait PauliYLike extends SingleQbitOperator {
  override def apply(row: Int, col: Int): Complex =
    (row, col) match {
      case (0, 1) => `-i`
      case (1, 0) => `i`
      case _ => `0`
    }

  override lazy val elements: Map[MatrixPos, Complex] = Map(
    MatrixPos(0, 1) -> `-i`,
    MatrixPos(1, 0) -> `i`
  )

  // overriden for simplier calculations
  override def apply(qbit: QbitValue): QbitValue = QbitValue(`-i` * qbit.`b|1>`, `1` * qbit.`a|0>`)

  override def toString: String = "PauliY"
}

object PauliY extends PauliYLike

object Y extends PauliYLike

trait PauliZLike extends SingleQbitOperator {
  override def apply(row: Int, col: Int): Complex = {
    (row, col) match {
      case (0, 0) => `1`
      case (1, 1) => `-1`
      case _ => `0`
    }
  }

  override lazy val elements: Map[MatrixPos, Complex] = Map(
    MatrixPos(0, 0) -> `1`,
    MatrixPos(1, 1) -> `-1`
  )

  // overriden for simplier calculations
  override def apply(qbit: QbitValue): QbitValue = qbit.copy(`b|1>` = `-1` * qbit.`b|1>`)

  override def toString: String = "PauliZ"
}

object PauliZ extends PauliZLike

object Z extends PauliZLike


// TODO next operators (2, 3 qbits, SWAP, CnNOT, itd)

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