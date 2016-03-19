package io.github.morgaroth.msc.quide.model.gates

/**
  * Created by mateusz on 11.01.16.
  */

case class ControlledGate(gate: SingleQbitGate, controlBit: Int) extends Gate {
  override def size: Int = 2
}