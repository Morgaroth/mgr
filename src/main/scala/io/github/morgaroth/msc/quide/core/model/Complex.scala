package io.github.morgaroth.msc.quide.core.model

import java.lang.Math._

/**
  * Created by mateusz on 04.01.16.
  */
case class Complex(re: Double, im: Double) extends Ordered[Complex] {
  private val modulus = sqrt(pow(re, 2) + pow(im, 2))

  // Constructors
  def this(re: Double) = this(re, 0)

  def non0 = re != 0d || im != 0d

  // Unary operators
  def unary_+ = this

  def unary_- = Complex(-re, -im)

  def unary_~ = Complex(re, -im)

  // conjugate
  def unary_! = modulus

  // Comparison
  def compare(that: Complex) = !this compare !that

  // Arithmetic operations
  def +(c: Complex) = Complex(re + c.re, im + c.im)

  def -(c: Complex) = this + -c

  def *(c: Complex) =
    Complex(re * c.re - im * c.im, im * c.re + re * c.im)

  def /(c: Complex) = {
    require(c.re != 0 || c.im != 0)
    val d = pow(c.re, 2) + pow(c.im, 2)
    Complex((re * c.re + im * c.im) / d, (im * c.re - re * c.im) / d)
  }

  // String representation
  override def toString() =
    this match {
      case Complex.i => "i"
      case Complex(r, 0) => r.toString
      case Complex(0, i) => i.toString + "*i"
      case _ => asString
    }

  private def asString =
    re + (if (im < 0) "-" + -im else "+" + im) + "*i"
}

object Complex {
  // Factory methods
  def apply(re: Double) = new Complex(re)

  // Implicit conversions
  implicit def fromDouble(d: Double): Complex = Complex(d)

  implicit def fromFloat(f: Float): Complex = Complex(f)

  implicit def fromLong(l: Long): Complex = Complex(l)

  implicit def fromInt(i: Int): Complex = Complex(i)

  implicit def fromShort(s: Short): Complex = Complex(s)


  // Constants
  val i = Complex(0, 1)
  val `-i` = Complex(0, -1)
  val zero = Complex(0)
  val `0` = zero
  val `1` = Complex(1)
  val `-1` = Complex(-1)
  val `1/p2` = Complex(1 / Math.sqrt(2))
  val `-1/p2` = Complex(-1 / Math.sqrt(2))
  val `1/2` = Complex(0.5)
  val `-1/2` = Complex(-0.5)
}
