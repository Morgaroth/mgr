package io.github.morgaroth.quide.core

/**
  * Created by mateusz on 04.01.16.
  */
package object model {
  //  type Complex = SymbolicComplex
  //  val Complex = SymbolicComplex
  type Complex = FloatComplex
  val Complex = FloatComplex

  type `|0>` = Complex
  type `|1>` = Complex
  type QValue = Complex
  val QValue = Complex
}
