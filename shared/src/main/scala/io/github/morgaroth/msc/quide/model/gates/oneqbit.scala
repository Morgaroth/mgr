package io.github.morgaroth.msc.quide.model.gates

import io.github.morgaroth.msc.quide.model.Complex._
import io.github.morgaroth.msc.quide.model.QValue

/**
  * Created by mateusz on 11.01.16.
  */

trait SingleQbitGate extends Gate {
  def execute(myValue: QValue, othValue: QValue, myQbit: Char): QValue = {
    myQbit match {
      case '0' => executeFor0(myValue, othValue)
      case '1' => executeFor1(othValue, myValue)
    }
  }

  private def throwInfo = throw new NotImplementedError("You have to override some logic here! Either execute (not using executeFor* methods), or both executeFor* methods.")

  protected def executeFor0(ampl0: QValue, ampl1: QValue): QValue = throwInfo

  protected def executeFor1(ampl0: QValue, ampl1: QValue): QValue = throwInfo

  override def size: Int = 1
}

/*
  Predefined, well-known operators
 */
trait IdentityLike extends SingleQbitGate {
  override def toString: String = "Identity"

  override def execute(myValue: QValue, othValue: QValue, myQbit: Char): QValue = myValue
}

object Intentity extends IdentityLike

object I extends IdentityLike

/*
  Haddammard Gate

       | 1    1 |
  1/p2 |        |
       | 1   -1 |
*/
trait HadammardLike extends SingleQbitGate {
  override def toString: String = "Hadammard"

  //     | ampl0 |
  // H * |       |
  //     | ampl1 |
  override protected def executeFor0(ampl0: QValue, ampl1: QValue) = (ampl0 + ampl1) * `1/p2`

  override protected def executeFor1(ampl0: QValue, ampl1: QValue) = (ampl0 - ampl1) * `1/p2`

}

object Hadammard extends HadammardLike

object H extends HadammardLike


/*
  Pauli X Gate

      | 0   1 |
  X = |       |
      | 1   0 |
*/
trait PauliXLike extends SingleQbitGate {
  override def toString: String = "PauliX"

  //     | ampl0 |
  // X * |       |
  //     | ampl1 |
  override protected def executeFor0(ampl0: QValue, ampl1: QValue): QValue = ampl1

  override protected def executeFor1(ampl0: QValue, ampl1: QValue): QValue = ampl0
}

object PauliX extends PauliXLike

object X extends PauliXLike


/*
  Pauli Y Gate

      | 0  -i |
  Y = |       |
      | i   0 |
*/
trait PauliYLike extends SingleQbitGate {
  override def toString: String = "PauliY"

  //     | ampl0 |
  // Y * |       |
  //     | ampl1 |
  override protected def executeFor0(ampl0: QValue, ampl1: QValue): QValue = ampl1 * `-i`

  override protected def executeFor1(ampl0: QValue, ampl1: QValue): QValue = ampl0 * `i`
}

object PauliY extends PauliYLike

object Y extends PauliYLike


/*
  Pauli Z Gate

      | 1   0 |
  Z = |       |
      | 0  -1 |
*/
trait PauliZLike extends SingleQbitGate {
  override def toString: String = "PauliZ"

  //     | ampl0 |
  // Z * |       |
  //     | ampl1 |
  override protected def executeFor0(ampl0: QValue, ampl1: QValue): QValue = ampl0

  override protected def executeFor1(ampl0: QValue, ampl1: QValue): QValue = -ampl1
}

object PauliZ extends PauliZLike

object Z extends PauliZLike