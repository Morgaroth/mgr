package io.github.morgaroth.msc.quide.model.gates

/**
  * Created by mateusz on 11.01.16.
  */
case class MultiControlledGate(gate: SingleQbitGate, controlBits: Set[Int]) extends Gate {
  override val size: Int = controlBits.size + 1
}