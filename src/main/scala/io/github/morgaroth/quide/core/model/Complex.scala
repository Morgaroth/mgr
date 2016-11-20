package io.github.morgaroth.quide.core.model

import java.lang.Math._

import scala.language.implicitConversions

/**
  * Created by mateusz on 04.01.16.
  */
case class FloatComplex(re: Double, im: Double) extends Ordered[FloatComplex] {

  val modulus = sqrt(pow(re, 2) + pow(im, 2))

  // Constructors
  def this(re: Double) = this(re, 0)

  def non0 = re != 0d || im != 0d

  // Unary operators
  def unary_+ = this

  def unary_- = FloatComplex(-re, -im)

  def unary_~ = FloatComplex(re, -im)

  // conjugate
  def unary_! = modulus

  // Comparison
  def compare(that: FloatComplex) = !this compare !that

  // Arithmetic operations
  def +(c: FloatComplex) = FloatComplex(re + c.re, im + c.im)

  def -(c: FloatComplex) = this + -c

  def *(c: FloatComplex) =
    FloatComplex(re * c.re - im * c.im, im * c.re + re * c.im)

  def /(c: FloatComplex) = {
    require(c.re != 0 || c.im != 0)
    val d = pow(c.re, 2) + pow(c.im, 2)
    FloatComplex((re * c.re + im * c.im) / d, (im * c.re - re * c.im) / d)
  }

  // String representation
  override def toString() = this match {
    case FloatComplex.i => "i"
    case FloatComplex(r, 0) => r.toString
    case FloatComplex(0, i) => i.toString + "*i"
    case _ => asString
  }

  def pretty = this match {
    case FloatComplex.i => "i"
    case FloatComplex(r, 0) => f"$r%1.3f"
    case FloatComplex(0, i) => f"$i%1.3f*i"
    case FloatComplex(r, i) => f"$r%1.3f + $i%1.3f*i"
  }


  def asString =
    re + (if (im < 0) "-" + -im else "+" + im) + "*i"
}

object FloatComplex {
  // Factory methods
  def apply(re: Double) = new FloatComplex(re)

  // Implicit conversions
  implicit def fromDouble(d: Double): FloatComplex = FloatComplex(d)

  implicit def fromFloat(f: Float): FloatComplex = FloatComplex(f)

  implicit def fromLong(l: Long): FloatComplex = FloatComplex(l)

  implicit def fromInt(i: Int): FloatComplex = FloatComplex(i)

  implicit def fromShort(s: Short): FloatComplex = FloatComplex(s)


  // Constants
  val i = FloatComplex(0, 1)
  val `-i` = FloatComplex(0, -1)
  val zero = FloatComplex(0)
  val `0` = zero
  val `1` = FloatComplex(1)
  val `-1` = FloatComplex(-1)
  val `1/p2` = FloatComplex(1 / Math.sqrt(2))
  val `-1/p2` = FloatComplex(-1 / Math.sqrt(2))
  val `1/2` = FloatComplex(0.5)
  val `-1/2` = FloatComplex(-0.5)
}

case class Fraction(num: Long, denum: Long) extends Ordered[Fraction] {
  assert(denum != 0, "denumerator cannot be 0")

  // Unary operators
  def unary_+ = this

  def unary_- = copy(num = -num)

  // Comparison
  def compare(c: Fraction) = {
    if (this.denum == c.denum) this.num compare c.num
    else this.num * c.denum compare c.num * this.denum
  }

  def minimized = {
    val gcd = BigInt(num).gcd(BigInt(denum)).longValue
    Fraction(num / gcd, denum / gcd)
  }

  // Arithmetic operations
  def +(c: Fraction) = {
    if (this.denum == c.denum) this.copy(num = num + c.num)
    else Fraction(
      this.num * c.denum + c.num * this.denum,
      this.denum * c.denum
    ).minimized
  }

  def -(c: Fraction) = this + -c

  def *(c: Fraction) =
    Fraction(this.num * c.num, this.denum * c.denum).minimized

  def /(c: Fraction) = {
    require(c.num != 0)
    Fraction(this.num * c.denum, this.denum * c.num).minimized
  }

  def doubleValue = num * 1.0d / denum
}

object Fraction {
  // Factory methods
  def apply(i: Long) = new Fraction(i, 1)

  // Implicit conversions
  implicit def fromDouble(d: Double): Fraction = Fraction((d * 10000000).toLong, 10000000)

  implicit def fromFloat(f: Float): Fraction = Fraction((f * 100000).toLong, 100000)

  implicit def fromLong(l: Long): Fraction = Fraction(l, 1)

  implicit def fromInt(i: Int): Fraction = Fraction(i, 1)
}

//object SymbolicComplex {
//  // Factory methods
//  def apply(re: Double) = new SymbolicComplex(re)
//
//  // Implicit conversions
//  implicit def fromDouble(d: Double): SymbolicComplex = SymbolicComplex(d)
//
//  implicit def fromFloat(f: Float): SymbolicComplex = SymbolicComplex(f)
//
//  implicit def fromLong(l: Long): SymbolicComplex = SymbolicComplex(l)
//
//  implicit def fromInt(i: Int): SymbolicComplex = SymbolicComplex(i)
//
//  implicit def fromShort(s: Short): SymbolicComplex = SymbolicComplex(s)
//
//
//  // Constants
//  val i = SymbolicComplex(0, 1)
//  val `-i` = SymbolicComplex(0, -1)
//  val zero = SymbolicComplex(0)
//  val `0` = zero
//  val `1` = SymbolicComplex(1)
//  val `-1` = SymbolicComplex(-1)
//  val `1/p2` = SymbolicComplex(1 / Math.sqrt(2))
//  val `-1/p2` = SymbolicComplex(-1 / Math.sqrt(2))
//  val `1/2` = SymbolicComplex(0.5)
//  val `-1/2` = SymbolicComplex(-0.5)
//}
//
//case class SymbolicComplex(re: Fraction, im: Fraction) extends Ordered[SymbolicComplex] {
//
//  val modulus = sqrt((re * re + im * im).doubleValue)
//
//  // Constructors
//  def this(re: Double) = this(re, 0)
//
//  def non0 = re != 0 || im != 0
//
//  // Unary operators
//  def unary_+ = this
//
//  def unary_- = SymbolicComplex(-re, -im)
//
//  def unary_~ = SymbolicComplex(re, -im)
//
//  // conjugate
//  def unary_! = modulus
//
//  // Comparison
//  def compare(that: SymbolicComplex) = !this compare !that
//
//  // Arithmetic operations
//  def +(c: SymbolicComplex) = SymbolicComplex(re + c.re, im + c.im)
//
//  def -(c: SymbolicComplex) = this + -c
//
//  def *(c: SymbolicComplex) =
//    SymbolicComplex(re * c.re - im * c.im, im * c.re + re * c.im)
//
//  def /(c: SymbolicComplex) = {
//    require(c.re != 0 || c.im != 0)
//    val d = pow(c.re, 2) + pow(c.im, 2)
//    SymbolicComplex((re * c.re + im * c.im) / d, (im * c.re - re * c.im) / d)
//  }
//
//  // String representation
//  override def toString() =
//    this match {
//      case SymbolicComplex.i => "i"
//      case SymbolicComplex(r, 0) => r.toString
//      case SymbolicComplex(0, i) => i.toString + "*i"
//      case _ => asString
//    }
//
//  //  def pretty = this match {
//  //    case SymbolicComplex.i => "i"
//  //    case SymbolicComplex(r, 0) => f"$r%1.3f"
//  //    case SymbolicComplex(0, i) => f"$i%1.3f*i"
//  //    case SymbolicComplex(r, i) => f"$r%1.3f + $i%1.3f*i"
//  //  }
//
//
//  private def asString =
//    re + (if (im < 0) "-" + -im else "+" + im) + "*i"
//}
