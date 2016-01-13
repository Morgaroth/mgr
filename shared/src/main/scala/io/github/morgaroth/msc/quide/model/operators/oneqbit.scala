package io.github.morgaroth.msc.quide.model.operators

import io.github.morgaroth.msc.quide.model.Complex._
import io.github.morgaroth.msc.quide.model.{Complex, QbitValue}

/**
  * Created by mateusz on 11.01.16.
  */

trait SingleQbitOperator extends Operator {
  override def size: Int = 1

  def apply(qbit: QbitValue): QbitValue = {
    QbitValue(this (0, 0) * qbit.a_0 + this (0, 1) * qbit.b_1, this (1, 0) * qbit.a_0 + this (1, 1) * qbit.b_1)
  }
}

object SingleQbitOperator {
  def apply(elems: List[(MatrixPos, Complex)], name: String = null) = new SingleQbitOperator {
    override def elements: Map[MatrixPos, Complex] = elems.toMap

    override def toString: String = Option(name).getOrElse(super.toString)
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

  override def apply(qbit: QbitValue): QbitValue = qbit

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
  override def apply(qbit: QbitValue): QbitValue = QbitValue(qbit.b_1, qbit.a_0)

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
  override def apply(qbit: QbitValue): QbitValue = QbitValue(`-i` * qbit.b_1, `1` * qbit.a_0)

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
  override def apply(qbit: QbitValue): QbitValue = qbit.copy(b_1 = `-1` * qbit.b_1)

  override def toString: String = "PauliZ"
}

object PauliZ extends PauliZLike

object Z extends PauliZLike
