package io.github.morgaroth.quide.core.model.gates

/**
  * Created by mateusz on 11.01.16.
  */
case class ControlledGate(gate: SingleQbitGate, controlBits: Set[Int]) extends Gate {
  override val size: Int = controlBits.size + 1
}

object ControlledGate {
  def apply(gate: SingleQbitGate, controlBits: Iterable[Int]): ControlledGate = new ControlledGate(gate, controlBits.toSet)
}