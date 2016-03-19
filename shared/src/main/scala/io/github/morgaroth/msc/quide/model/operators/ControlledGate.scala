package io.github.morgaroth.msc.quide.model.operators

/**
  * Created by mateusz on 11.01.16.
  */

case class ControlledGate(operator: SingleQbitOperator, controlBit: Int, targetBit: Int) extends Operator {
  override def size: Int = 2
}